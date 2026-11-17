package com.thecrudify.zdownload.data.repository

import android.content.Context
import android.os.Environment
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.ZDownloadApp
import com.thecrudify.zdownload.data.model.TrackInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class YoutubeDownloaderRepository(private val context: Context) {

    // Engine init failures can be Errors (e.g. ExceptionInInitializerError); turn them into a shown message, not a crash
    private suspend fun awaitEngines() = try {
        (context.applicationContext as ZDownloadApp).enginesReady.await()
    } catch (e: Error) {
        throw IllegalStateException("Engine gagal dimuat: ${e.message}", e)
    }

    suspend fun fetchTrackInfo(url: String): Result<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            awaitEngines()
            val request = YoutubeDLRequest(url)
            request.addOption("--extractor-args", "youtube:player_client=android")
            // Share links often carry &list=…; without this yt-dlp resolves the whole playlist
            request.addOption("--no-playlist")
            val videoInfo = YoutubeDL.getInstance().getInfo(request)
            val track = TrackInfo(
                id = videoInfo.id ?: "",
                title = videoInfo.title ?: "Unknown Track",
                uploader = videoInfo.uploader ?: "Unknown Artist",
                durationSeconds = videoInfo.duration.toLong(),
                thumbnailUrl = videoInfo.thumbnail ?: "",
                originalUrl = url
            )
            Result.success(track)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadTrack(
        trackInfo: TrackInfo,
        format: AudioFormat,
        onProgress: (progress: Float, status: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            awaitEngines()
            // Save to standard Music directory or Downloads
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val targetDir = File(musicDir, "ZDownload")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val request = YoutubeDLRequest(trackInfo.originalUrl)
            request.addOption("--extractor-args", "youtube:player_client=android")
            request.addOption("-x")
            request.addOption("--audio-format", format.formatKey)
            request.addOption("--audio-quality", "0")
            request.addOption("--embed-thumbnail")
            request.addOption("--add-metadata")
            request.addOption("--parse-metadata", "uploader:%(artist)s")
            request.addOption("--parse-metadata", "title:%(title)s")
            request.addOption("-o", "${targetDir.absolutePath}/%(title)s.%(ext)s")
            request.addOption("--no-playlist")

            var lastReportedFile: File? = null

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                val progressFloat = (progress / 100f).coerceIn(0f, 1f)
                onProgress(progressFloat, line ?: "Mengekstrak audio...")
            }

            // Find the downloaded file in the target directory
            val files = targetDir.listFiles()?.filter {
                it.isFile && it.name.contains(trackInfo.id, ignoreCase = true) ||
                        it.lastModified() > System.currentTimeMillis() - 30_000
            }?.sortedByDescending { it.lastModified() }

            val resultFile = files?.firstOrNull() ?: File(targetDir, "${trackInfo.title}.${format.extension}")

            Result.success(resultFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
