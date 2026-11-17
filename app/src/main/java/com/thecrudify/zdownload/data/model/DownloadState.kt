package com.thecrudify.zdownload.data.model

import java.io.File

sealed interface DownloadState {
    object Idle : DownloadState
    data class FetchingInfo(val message: String = "Mengambil informasi lagu...") : DownloadState
    data class TrackLoaded(val trackInfo: TrackInfo) : DownloadState
    data class Downloading(
        val trackInfo: TrackInfo,
        val progress: Float,
        val statusMessage: String
    ) : DownloadState
    data class Success(
        val trackInfo: TrackInfo,
        val file: File,
        val fileName: String
    ) : DownloadState
    data class Error(val message: String) : DownloadState
}
