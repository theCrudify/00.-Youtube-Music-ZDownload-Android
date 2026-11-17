package com.thecrudify.zdownload.data.repository

import android.content.Context
import com.thecrudify.zdownload.ZDownloadApp
import com.thecrudify.zdownload.data.download.YtDlp
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.model.TrackInfo
import com.thecrudify.zdownload.data.storage.safeFileName
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class DownloadedFiles(val audio: File, val artwork: File?)

/**
 * The yt-dlp engine: resolves a link and produces an audio file in a private work folder.
 * Queueing, states and the library are handled by DownloadManager / LibraryRepository.
 *
 * Note: do NOT force `--extractor-args youtube:player_client=…`. With `android` YouTube only
 * offers format 18 (360p video, ~96kbps audio): 3x the data and lower quality than the
 * audio-only Opus/M4A streams yt-dlp's default client selection gets.
 */
class YoutubeDownloaderRepository(private val context: Context) {

    // yt-dlp's metadata from analysis, reused by the download so it doesn't extract twice
    private fun infoFile(sourceId: String) =
        File(File(context.cacheDir, "info").apply { mkdirs() }, safeFileName(sourceId) + ".json")

    /** Per download, so a paused/failed one keeps its `.part` file and resumes instead of restarting. */
    fun workDir(sourceId: String, format: AudioFormat) =
        File(File(context.cacheDir, "download"), workDirName(sourceId, format))

    fun workDirName(sourceId: String, format: AudioFormat) = safeFileName(sourceId) + "-" + format.name

    // Engine init failures can be Errors (e.g. ExceptionInInitializerError); turn them into a shown message, not a crash
    private suspend fun awaitEngines() = try {
        (context.applicationContext as ZDownloadApp).enginesReady.await()
    } catch (e: Error) {
        throw IllegalStateException("Engine gagal dimuat: ${e.message}", e)
    }

    suspend fun fetchTrackInfo(url: String): Result<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            require(YtDlp.isSupportedUrl(url)) { "Link harus dari YouTube atau YouTube Music." }
            awaitEngines()
            val request = YoutubeDLRequest(url)
            request.addOption("--dump-json")
            // Share links often carry &list=…; without this yt-dlp resolves the whole playlist
            request.addOption("--no-playlist")
            val json = YoutubeDL.getInstance().execute(request).out
            val info = JSONObject(json)
            val track = TrackInfo(
                id = info.optString("id"),
                title = info.optString("title", "Unknown Track"),
                uploader = info.optString("uploader", "Unknown Artist"),
                durationSeconds = info.optDouble("duration", 0.0).toLong(),
                thumbnailUrl = info.optString("thumbnail"),
                originalUrl = url
            )
            require(track.id.isNotBlank()) { "yt-dlp tidak mengembalikan ID video." }
            infoFile(track.id).writeText(json)
            Result.success(track)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Runs yt-dlp to completion and returns the files it left in the work folder.
     * Throws when yt-dlp fails or the process is destroyed via [stop].
     *
     * @param isStopped true once the caller asked to pause/cancel, so a kill isn't retried as a failure
     */
    suspend fun download(
        sourceId: String,
        sourceUrl: String,
        format: AudioFormat,
        processId: String,
        isStopped: () -> Boolean,
        onProgress: (fraction: Float, etaSeconds: Long, line: String) -> Unit
    ): DownloadedFiles = withContext(Dispatchers.IO) {
        awaitEngines()
        // Private cache, not the music folder: yt-dlp drops .part/.webp/.json helpers next to the output
        val workDir = workDir(sourceId, format).apply { mkdirs() }
        val info = infoFile(sourceId)

        fun run(reuseInfo: Boolean) {
            val request = if (reuseInfo) {
                YoutubeDLRequest(emptyList()).apply { addOption("--load-info-json", info.absolutePath) }
            } else {
                YoutubeDLRequest(sourceUrl)
            }
            request.addOption("-x")
            request.addOption("--audio-format", format.formatKey)
            request.addOption("--audio-quality", "0")
            // Keep the cover file as well as embedding it: the library shows it without re-reading the audio
            request.addOption("--write-thumbnail")
            request.addOption("--embed-thumbnail")
            request.addOption("--add-metadata")
            request.addOption("--parse-metadata", "uploader:%(artist)s")
            request.addOption("--parse-metadata", "title:%(title)s")
            request.addOption("--trim-filenames", "100")
            request.addOption("-o", "${workDir.absolutePath}/%(title)s.%(ext)s")
            request.addOption("--no-playlist")

            YoutubeDL.getInstance().execute(request, processId) { progress, eta, line ->
                onProgress((progress / 100f).coerceIn(0f, 1f), eta, line)
            }
        }

        val canReuseInfo = info.exists()
        try {
            run(reuseInfo = canReuseInfo)
        } catch (e: YoutubeDLException) {
            // Saved stream URLs expire after a few hours; extract again from the link
            if (!canReuseInfo || isStopped()) throw e
            run(reuseInfo = false)
        }

        val files = workDir.listFiles()?.filter { it.isFile }.orEmpty()
        val audio = files.filter { it.extension.lowercase() !in HELPER_EXTENSIONS }.maxByOrNull { it.length() }
            ?: error("yt-dlp selesai tanpa menghasilkan file audio")
        DownloadedFiles(audio, files.firstOrNull { it.extension.lowercase() in IMAGE_EXTENSIONS })
    }

    /** Kills the running yt-dlp process for [processId]; its `.part` file stays for a later resume. */
    fun stop(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }

    fun clearWork(sourceId: String, format: AudioFormat) {
        workDir(sourceId, format).deleteRecursively()
    }

    private companion object {
        val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
        val HELPER_EXTENSIONS = IMAGE_EXTENSIONS + setOf("part", "ytdl", "json", "tmp")
    }
}
