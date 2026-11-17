package com.thecrudify.zdownload.data.download

import android.os.SystemClock
import com.thecrudify.zdownload.data.db.DownloadEntity
import com.thecrudify.zdownload.data.db.DownloadStatus
import com.thecrudify.zdownload.data.db.LibraryDao
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.metadata.MetadataReader
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.model.TrackInfo
import com.thecrudify.zdownload.data.repository.LibraryRepository
import com.thecrudify.zdownload.data.repository.YoutubeDownloaderRepository
import com.thecrudify.zdownload.data.storage.MusicStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Progress of a running download. Kept in memory only: it changes several times a second. */
data class LiveProgress(val fraction: Float, val speed: String?, val etaSeconds: Long?)

sealed interface EnqueueResult {
    data class Queued(val downloadId: Long) : EnqueueResult
    data class AlreadyQueued(val downloadId: Long) : EnqueueResult
    data class AlreadyInLibrary(val track: Track) : EnqueueResult
}

/**
 * The download queue. Every download is a row in Room (so history survives restarts) that moves
 * PENDING → DOWNLOADING → VERIFYING → EXTRACTING_METADATA → REGISTERING_LIBRARY → COMPLETED,
 * or ends FAILED / CANCELLED / PAUSED.
 *
 * Lives in the application scope, not a screen: [onWorkAvailable] starts the foreground service
 * that keeps the process alive while the queue is busy.
 */
class DownloadManager(
    private val dao: LibraryDao,
    private val engine: YoutubeDownloaderRepository,
    private val library: LibraryRepository,
    private val storage: MusicStorage,
    private val scope: CoroutineScope,
    private val onWorkAvailable: () -> Unit
) {
    private val _live = MutableStateFlow<Map<Long, LiveProgress>>(emptyMap())
    val live: StateFlow<Map<Long, LiveProgress>> = _live

    private val queue = Channel<Long>(Channel.UNLIMITED)

    /** Set when the user pauses/cancels a running download, so the killed process isn't reported as a failure. */
    private val stopRequests = ConcurrentHashMap<Long, DownloadStatus>()
    private val running = ConcurrentHashMap.newKeySet<Long>()

    fun start() {
        scope.launch {
            // Whatever was mid-flight when the process died did not finish. Its .part file is kept, so Retry resumes.
            dao.getActiveDownloads().forEach {
                setStatus(it, DownloadStatus.FAILED, error = "Terputus saat aplikasi ditutup. Ketuk Ulangi untuk melanjutkan.")
            }
            repeat(MAX_CONCURRENT) { launch { for (id in queue) process(id) } }
        }
    }

    private fun processId(id: Long) = "download-$id"

    private suspend fun setStatus(
        download: DownloadEntity,
        status: DownloadStatus,
        error: String? = null,
        trackId: String? = download.trackId,
        fileSize: Long? = download.fileSize
    ): DownloadEntity {
        val updated = download.copy(
            status = status.name, error = error, trackId = trackId, fileSize = fileSize,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateDownload(updated)
        return updated
    }

    /** Adds a song to the queue unless it is already downloaded or already queued. */
    suspend fun enqueue(info: TrackInfo, format: AudioFormat): EnqueueResult {
        val existing = dao.findDownload(info.id, format.name)
        if (existing != null) {
            if (existing.statusEnum.isActive) return EnqueueResult.AlreadyQueued(existing.id)
            val track = existing.trackId?.let { dao.getTrack(it) }
            if (existing.statusEnum == DownloadStatus.COMPLETED && track != null && File(track.entity.filePath).isFile) {
                return EnqueueResult.AlreadyInLibrary(track)
            }
            // A finished/failed record for the same song: reuse the row instead of adding a second one
            setStatus(existing, DownloadStatus.PENDING)
            submit(existing.id)
            return EnqueueResult.Queued(existing.id)
        }
        val now = System.currentTimeMillis()
        val id = dao.insertDownload(
            DownloadEntity(
                sourceId = info.id, sourceUrl = info.originalUrl, title = info.title, artist = info.uploader,
                artworkUrl = info.thumbnailUrl, durationSec = info.durationSeconds, format = format.name,
                status = DownloadStatus.PENDING.name, createdAt = now, updatedAt = now
            )
        )
        submit(id)
        return EnqueueResult.Queued(id)
    }

    private fun submit(id: Long) {
        queue.trySend(id)
        onWorkAvailable()
    }

    /** Retry a failed/cancelled download or resume a paused one. yt-dlp continues from the kept .part file. */
    suspend fun retry(id: Long) {
        val download = dao.getDownload(id) ?: return
        if (download.statusEnum.isActive || download.statusEnum == DownloadStatus.COMPLETED) return
        setStatus(download, DownloadStatus.PENDING)
        submit(id)
    }

    suspend fun pause(id: Long) = stop(id, DownloadStatus.PAUSED)
    suspend fun cancel(id: Long) = stop(id, DownloadStatus.CANCELLED)

    suspend fun cancelAll() = dao.getActiveDownloads().forEach { cancel(it.id) }

    private suspend fun stop(id: Long, target: DownloadStatus) {
        val download = dao.getDownload(id) ?: return
        if (id in running) {
            stopRequests[id] = target
            engine.stop(processId(id))
        } else if (download.statusEnum == DownloadStatus.PENDING || download.statusEnum == DownloadStatus.PAUSED) {
            setStatus(download, target)
            if (target == DownloadStatus.CANCELLED) engine.clearWork(download.sourceId, AudioFormat.valueOf(download.format))
        }
    }

    /** Deletes the record (not the song). Active downloads must be cancelled first. */
    suspend fun remove(id: Long) {
        val download = dao.getDownload(id) ?: return
        if (download.statusEnum.isActive) return
        engine.clearWork(download.sourceId, AudioFormat.valueOf(download.format))
        dao.deleteDownload(id)
    }

    suspend fun clearFinished() = dao.deleteFinishedDownloads()

    /** Work folders that must survive a "clear temporary files": anything not finished may still resume. */
    suspend fun workDirsInUse(): Set<String> =
        dao.getActiveDownloads().map { engine.workDirName(it.sourceId, AudioFormat.valueOf(it.format)) }.toSet()

    private suspend fun process(id: Long) {
        var download = dao.getDownload(id) ?: return
        if (download.statusEnum != DownloadStatus.PENDING) return // cancelled or paused while waiting
        val format = AudioFormat.valueOf(download.format)
        running += id
        try {
            val free = storage.freeBytes()
            if (free < LibraryRepository.MIN_FREE_BYTES) {
                throw IOException("Penyimpanan hampir penuh (sisa ${free / 1_048_576} MB). Kosongkan ruang lalu ulangi.")
            }
            download = setStatus(download, DownloadStatus.DOWNLOADING)

            var lastEmit = 0L
            val files = engine.download(
                sourceId = download.sourceId, sourceUrl = download.sourceUrl, format = format,
                processId = processId(id), isStopped = { stopRequests.containsKey(id) }
            ) { fraction, eta, line ->
                // yt-dlp prints many lines per second; update observers at most 4x/s
                val now = SystemClock.uptimeMillis()
                if (now - lastEmit >= 250 || fraction >= 1f) {
                    lastEmit = now
                    val speed = YtDlp.parseSpeed(line) ?: _live.value[id]?.speed
                    _live.update { it + (id to LiveProgress(fraction, speed, eta.takeIf { s -> s > 0 })) }
                }
            }
            if (stopRequests.containsKey(id)) throw IOException("stopped")

            download = setStatus(download, DownloadStatus.VERIFYING)
            _live.update { it - id }
            // Throws if the result is empty, truncated or not decodable audio
            val meta = MetadataReader.read(files.audio)

            download = setStatus(download, DownloadStatus.EXTRACTING_METADATA)
            // (tags and cover were read by the verification pass above; yt-dlp embedded them)

            download = setStatus(download, DownloadStatus.REGISTERING_LIBRARY)
            val track = library.registerDownload(download, files.audio, files.artwork, meta)

            setStatus(download, DownloadStatus.COMPLETED, trackId = track.id, fileSize = track.entity.fileSize)
            engine.clearWork(download.sourceId, format)
        } catch (e: Exception) {
            val current = dao.getDownload(id) ?: return
            when (stopRequests[id]) {
                DownloadStatus.PAUSED -> setStatus(current, DownloadStatus.PAUSED)
                DownloadStatus.CANCELLED -> {
                    setStatus(current, DownloadStatus.CANCELLED)
                    engine.clearWork(current.sourceId, format) // incomplete files of a cancelled download are useless
                }
                else -> setStatus(current, DownloadStatus.FAILED, error = YtDlp.summarizeError(e.message))
            }
        } finally {
            stopRequests.remove(id)
            running -= id
            _live.update { it - id }
        }
    }

    private companion object {
        /** Each download is a Python + FFmpeg process; two is what a low-end phone handles comfortably. */
        const val MAX_CONCURRENT = 2
    }
}
