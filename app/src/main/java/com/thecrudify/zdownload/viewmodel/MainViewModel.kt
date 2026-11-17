package com.thecrudify.zdownload.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thecrudify.zdownload.ZDownloadApp
import com.thecrudify.zdownload.data.db.DownloadStatus
import com.thecrudify.zdownload.data.download.EnqueueResult
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.model.DownloadState
import com.thecrudify.zdownload.data.model.TrackInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/** The "add a song" flow on Home: paste a link, analyze it, pick a format, hand it to the download queue. */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ZDownloadApp

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _selectedFormat = MutableStateFlow(AudioFormat.ORIGINAL)
    val selectedFormat: StateFlow<AudioFormat> = _selectedFormat.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /** Fired when a link arrives from another app's Share menu, so the UI can switch to Home. */
    private val _sharedLinks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sharedLinks: SharedFlow<Unit> = _sharedLinks

    private var analyzeJob: Job? = null
    private var watchJob: Job? = null

    init {
        viewModelScope.launch {
            _selectedFormat.value = runCatching { AudioFormat.valueOf(app.prefs.defaultFormat.first()) }
                .getOrDefault(AudioFormat.ORIGINAL)
        }
    }

    fun onUrlChanged(newUrl: String) {
        _url.value = newUrl
    }

    fun onSharedLink(url: String) {
        _url.value = url
        _sharedLinks.tryEmit(Unit)
        analyzeUrl()
    }

    fun clearUrl() {
        analyzeJob?.cancel()
        watchJob?.cancel() // stops watching only; the download itself keeps running in the queue
        _url.value = ""
        _downloadState.value = DownloadState.Idle
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            _url.value = text
            analyzeUrl()
        }
    }

    fun selectFormat(format: AudioFormat) {
        _selectedFormat.value = format
        viewModelScope.launch { app.prefs.setDefaultFormat(format.name) }
    }

    fun analyzeUrl() {
        val currentUrl = _url.value.trim()
        if (currentUrl.isEmpty()) {
            _downloadState.value = DownloadState.Error("Masukkan link YouTube atau YouTube Music terlebih dahulu.")
            return
        }
        watchJob?.cancel()
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _downloadState.value = DownloadState.FetchingInfo("Mengambil informasi lagu dari YouTube...")
            app.engine.fetchTrackInfo(currentUrl)
                .onSuccess { _downloadState.value = DownloadState.TrackLoaded(it) }
                .onFailure { _downloadState.value = DownloadState.Error("Gagal mengambil info: ${it.localizedMessage ?: "Terjadi kesalahan"}") }
        }
    }

    /** Queues the download and mirrors its real state onto this screen. The work itself runs in DownloadManager. */
    fun startDownload(trackInfo: TrackInfo) {
        val format = _selectedFormat.value
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            when (val result = app.downloads.enqueue(trackInfo, format)) {
                is EnqueueResult.AlreadyInLibrary -> _downloadState.value = success(trackInfo, result.track)
                is EnqueueResult.Queued -> watch(result.downloadId, trackInfo)
                is EnqueueResult.AlreadyQueued -> watch(result.downloadId, trackInfo)
            }
        }
    }

    private fun success(info: TrackInfo, track: Track): DownloadState.Success {
        val file = File(track.entity.filePath)
        return DownloadState.Success(trackInfo = info, file = file, fileName = file.name, trackId = track.id)
    }

    private suspend fun watch(downloadId: Long, info: TrackInfo) {
        combine(app.database.dao().observeDownload(downloadId), app.downloads.live) { download, live ->
            download to live[downloadId]
        }.collect { (download, progress) ->
            if (download == null) {
                _downloadState.value = DownloadState.TrackLoaded(info)
                return@collect
            }
            _downloadState.value = when (download.statusEnum) {
                DownloadStatus.COMPLETED -> download.trackId?.let { app.library.getTrack(it) }?.let { success(info, it) }
                    ?: DownloadState.Error("Lagu selesai diunduh tetapi tidak ditemukan di library.")
                DownloadStatus.FAILED -> DownloadState.Error("Gagal mengunduh: ${download.error ?: "Terjadi kesalahan"}")
                DownloadStatus.CANCELLED, DownloadStatus.PAUSED -> DownloadState.TrackLoaded(info)
                DownloadStatus.PENDING -> DownloadState.Downloading(info, 0f, "Menunggu giliran di antrean...")
                DownloadStatus.DOWNLOADING -> DownloadState.Downloading(
                    info, progress?.fraction ?: 0f,
                    progress?.speed?.let { "Mengunduh • $it" } ?: "Menyiapkan unduhan..."
                )
                DownloadStatus.VERIFYING -> DownloadState.Downloading(info, 1f, "Memeriksa file...")
                DownloadStatus.EXTRACTING_METADATA -> DownloadState.Downloading(info, 1f, "Membaca metadata...")
                DownloadStatus.REGISTERING_LIBRARY -> DownloadState.Downloading(info, 1f, "Menyimpan ke library...")
            }
        }
    }

    fun playDownloaded(trackId: String) {
        viewModelScope.launch {
            app.library.getTrack(trackId)?.let { app.playback.play(listOf(it)) }
        }
    }
}
