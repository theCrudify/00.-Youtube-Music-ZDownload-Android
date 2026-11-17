package com.thecrudify.zdownload

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class ZDownloadApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Completes when yt-dlp & FFmpeg are ready (and yt-dlp updated, at most once a day). Await before any YoutubeDL call (share intents can arrive first). */
    lateinit var enginesReady: Deferred<Unit>
        private set

    override fun onCreate() {
        super.onCreate()
        enginesReady = appScope.async {
            YoutubeDL.getInstance().init(this@ZDownloadApp)
            FFmpeg.getInstance().init(this@ZDownloadApp)
            Log.d(TAG, "YoutubeDL & FFmpeg initialized.")
            // Update before signalling ready: replacing yt-dlp while a request runs makes that request fail
            updateYtDlpIfStale()
        }
    }

    // yt-dlp must stay fresh to keep up with YouTube, but checking on every launch wastes data
    private fun updateYtDlpIfStale() {
        val prefs = getSharedPreferences("engine", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_UPDATE, 0L) < UPDATE_INTERVAL_MS) return
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(this)
            prefs.edit().putLong(KEY_LAST_UPDATE, now).apply()
            Log.d(TAG, "yt-dlp update: $status")
        } catch (e: Throwable) {
            Log.w(TAG, "yt-dlp update skipped: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "ZDownloadApp"
        const val KEY_LAST_UPDATE = "ytdlp_last_update"
        const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
