package com.thecrudify.zdownload.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaScannerConnection
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.model.DownloadState
import com.thecrudify.zdownload.data.model.TrackInfo
import com.thecrudify.zdownload.data.repository.YoutubeDownloaderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = YoutubeDownloaderRepository(application)

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _selectedFormat = MutableStateFlow(AudioFormat.ORIGINAL)
    val selectedFormat: StateFlow<AudioFormat> = _selectedFormat.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _url.value = newUrl
    }

    fun clearUrl() {
        _url.value = ""
        _downloadState.value = DownloadState.Idle
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                _url.value = text
                analyzeUrl()
            }
        }
    }

    fun selectFormat(format: AudioFormat) {
        _selectedFormat.value = format
    }

    fun analyzeUrl() {
        val currentUrl = _url.value.trim()
        if (currentUrl.isEmpty()) {
            _downloadState.value = DownloadState.Error("Masukkan link YouTube atau YouTube Music terlebih dahulu.")
            return
        }

        viewModelScope.launch {
            _downloadState.value = DownloadState.FetchingInfo("Mengambil informasi lagu dari YouTube...")
            val result = repository.fetchTrackInfo(currentUrl)
            result.onSuccess { track ->
                _downloadState.value = DownloadState.TrackLoaded(track)
            }.onFailure { error ->
                _downloadState.value = DownloadState.Error("Gagal mengambil info: ${error.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun startDownload(trackInfo: TrackInfo) {
        val format = _selectedFormat.value
        var lastProgressEmit = 0L
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(
                trackInfo = trackInfo,
                progress = 0f,
                statusMessage = "Mengekstrak audio ${format.title}..."
            )

            val result = repository.downloadTrack(
                trackInfo = trackInfo,
                format = format
            ) { progress, status ->
                // yt-dlp prints many lines per second; redraw the screen at most 5x/s
                val now = SystemClock.uptimeMillis()
                if (now - lastProgressEmit < 200 && progress < 1f) return@downloadTrack
                lastProgressEmit = now
                _downloadState.value = DownloadState.Downloading(
                    trackInfo = trackInfo,
                    progress = progress,
                    statusMessage = status
                )
            }

            result.onSuccess { downloadedFile ->
                // Refresh Android MediaStore so file appears in Music Player
                MediaScannerConnection.scanFile(
                    getApplication(),
                    arrayOf(downloadedFile.absolutePath),
                    null,
                    null
                )

                _downloadState.value = DownloadState.Success(
                    trackInfo = trackInfo,
                    file = downloadedFile,
                    fileName = downloadedFile.name
                )
            }.onFailure { error ->
                _downloadState.value = DownloadState.Error("Gagal mengunduh: ${error.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }
}
