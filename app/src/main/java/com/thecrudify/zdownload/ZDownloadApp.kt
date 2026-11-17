package com.thecrudify.zdownload

import android.app.Application
import android.util.Log
import com.thecrudify.zdownload.data.db.AppDatabase
import com.thecrudify.zdownload.data.download.DownloadManager
import com.thecrudify.zdownload.data.download.DownloadService
import com.thecrudify.zdownload.data.prefs.AppPreferences
import com.thecrudify.zdownload.ui.AppText
import com.thecrudify.zdownload.ui.LocaleHelper
import com.thecrudify.zdownload.data.repository.LibraryRepository
import com.thecrudify.zdownload.data.repository.YoutubeDownloaderRepository
import com.thecrudify.zdownload.data.storage.MusicStorage
import com.thecrudify.zdownload.playback.PlaybackController
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class ZDownloadApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // App-wide singletons (manual dependency injection): created on first use, shared by every screen and service
    val database by lazy { AppDatabase.create(this) }
    val prefs by lazy { AppPreferences(this) }
    val storage by lazy { MusicStorage(this) }
    val engine by lazy { YoutubeDownloaderRepository(this) }
    val library by lazy { LibraryRepository(this, database.dao(), storage) }
    val downloads by lazy {
        DownloadManager(database.dao(), engine, library, storage, appScope) { DownloadService.start(this) }
    }
    val playback by lazy { PlaybackController(this, library, prefs, mainScope) }

    /** Completes when yt-dlp & FFmpeg are ready (and yt-dlp updated, at most once a day). Await before any YoutubeDL call (share intents can arrive first). */
    lateinit var enginesReady: Deferred<Unit>
        private set

    override fun onCreate() {
        super.onCreate()
        // Keeps text built outside Compose (messages, notifications) in the chosen language.
        mainScope.launch {
            prefs.language.collect { AppText.lang = LocaleHelper.toAppLanguage(it) }
        }
        enginesReady = appScope.async {
            YoutubeDL.getInstance().init(this@ZDownloadApp)
            FFmpeg.getInstance().init(this@ZDownloadApp)
            Log.d(TAG, "YoutubeDL & FFmpeg initialized.")
            // Update before signalling ready: replacing yt-dlp while a request runs makes that request fail
            updateYtDlpIfStale()
        }
        downloads.start()
        playback.connect()
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
