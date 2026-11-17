package com.thecrudify.zdownload.data.repository

import android.content.Context
import android.os.Environment
import com.thecrudify.zdownload.ZDownloadApp
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.model.TrackInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

// Note: do NOT force `--extractor-args youtube:player_client=…`. With `android` YouTube only
// offers format 18 (360p video, ~96kbps audio): 3x the data and lower quality than the
// audio-only Opus/M4A streams yt-dlp's default client selection gets.
class YoutubeDownloaderRepository(private val context: Context) {

    // yt-dlp's metadata from the last analysis, reused by the download so it doesn't extract twice
    private val infoFile = File(context.cacheDir, "trackinfo.json")
    private var infoFileTrackId: String? = null

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
            infoFile.writeText(json)
            infoFileTrackId = track.id
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
            val workDir = File(context.cacheDir, "download")

            fun run(reuseInfo: Boolean) {
                workDir.deleteRecursively()
                workDir.mkdirs()
                val request = if (reuseInfo) {
                    YoutubeDLRequest(emptyList()).apply { addOption("--load-info-json", infoFile.absolutePath) }
                } else {
                    YoutubeDLRequest(trackInfo.originalUrl)
                }
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
                    val progressFloat = (progress / 100f).coerceIn(0f, 1f)
                    onProgress(progressFloat, line ?: "Mengekstrak audio...")
                }
            }

            val canReuseInfo = infoFileTrackId == trackInfo.id && infoFile.exists()
            try {
                run(reuseInfo = canReuseInfo)
            } catch (e: YoutubeDLException) {
                // Saved stream URLs expire after a few hours; extract again from the link
                if (!canReuseInfo) throw e
                run(reuseInfo = false)
            }

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
