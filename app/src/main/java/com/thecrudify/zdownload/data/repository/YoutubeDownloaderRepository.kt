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
            android.util.Log.d("ZDL_MEASURE", "INFO_START") // TEMP-MEASURE
            val request = YoutubeDLRequest(url)
            java.io.File(context.filesDir, "args").takeIf { it.exists() }?.readText()?.trim().let { c -> if (c == null) request.addOption("--extractor-args", "youtube:player_client=android") else if (c != "none") request.addOption("--extractor-args", "youtube:player_client=$c") } // TEMP-MEASURE
            // Share links often carry &list=…; without this yt-dlp resolves the whole playlist
            request.addOption("--no-playlist")
            val videoInfo = YoutubeDL.getInstance().getInfo(request)
            videoInfo.formats?.forEach { f -> android.util.Log.d("ZDL_MEASURE", "FMT ${f.formatId} a=${f.acodec} v=${f.vcodec} abr=${f.abr} tbr=${f.tbr} ext=${f.ext}") } // TEMP-MEASURE
            android.util.Log.d("ZDL_MEASURE", "INFO_END") // TEMP-MEASURE
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
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val targetDir = File(musicDir, "ZDownload").apply { mkdirs() }

            // Work in private cache: scoped storage only allows audio files in Music/, and yt-dlp
            // writes the cover as .webp there before embedding it, which fails with EPERM.
            val workDir = File(context.cacheDir, "download").apply {
                deleteRecursively()
                mkdirs()
            }

            android.util.Log.d("ZDL_MEASURE", "DL_START") // TEMP-MEASURE
            val request = YoutubeDLRequest(trackInfo.originalUrl)
            java.io.File(context.filesDir, "args").takeIf { it.exists() }?.readText()?.trim().let { c -> if (c == null) request.addOption("--extractor-args", "youtube:player_client=android") else if (c != "none") request.addOption("--extractor-args", "youtube:player_client=$c") } // TEMP-MEASURE
            request.addOption("-x")
            request.addOption("--audio-format", format.formatKey)
            request.addOption("--audio-quality", "0")
            request.addOption("--embed-thumbnail")
            request.addOption("--add-metadata")
            request.addOption("--parse-metadata", "uploader:%(artist)s")
            request.addOption("--parse-metadata", "title:%(title)s")
            request.addOption("-o", "${workDir.absolutePath}/%(title)s.%(ext)s")
            request.addOption("--no-playlist")

            YoutubeDL.getInstance().execute(request) { progress, _, line ->
                android.util.Log.d("ZDL_MEASURE", line ?: "") // TEMP-MEASURE
                val progressFloat = (progress / 100f).coerceIn(0f, 1f)
                onProgress(progressFloat, line ?: "Mengekstrak audio...")
            }

            android.util.Log.d("ZDL_MEASURE", "DL_END") // TEMP-MEASURE
            // The finished audio is the largest file left (cover art is embedded and removed)
            val produced = workDir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.length() }
                ?: error("yt-dlp selesai tanpa menghasilkan file audio")
            val resultFile = produced.copyTo(File(targetDir, produced.name), overwrite = true)
            workDir.deleteRecursively()

            Result.success(resultFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
