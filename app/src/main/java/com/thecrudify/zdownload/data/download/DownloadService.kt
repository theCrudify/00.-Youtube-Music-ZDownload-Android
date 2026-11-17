package com.thecrudify.zdownload.data.download

import com.thecrudify.zdownload.ui.t
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.thecrudify.zdownload.MainActivity
import com.thecrudify.zdownload.ZDownloadApp
import com.thecrudify.zdownload.data.db.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps the process alive (and the user informed) while the download queue is busy.
 * A foreground service rather than WorkManager: downloads are user-started, should run now,
 * and can outlast WorkManager's 10-minute window on a slow connection.
 * It only hosts the notification; the queue itself is DownloadManager.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as ZDownloadApp
        if (intent?.action == ACTION_CANCEL_ALL) {
            scope.launch { app.downloads.cancelAll() }
            return START_NOT_STICKY
        }
        startInForeground(notification(emptyList(), emptyMap()))
        if (watcher == null) {
            watcher = scope.launch {
                combine(app.database.dao().observeDownloads(), app.downloads.live) { all, live ->
                    all.filter { it.statusEnum.isActive } to live
                }.collect { (active, live) ->
                    if (active.isEmpty()) {
                        ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(active, live))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        // Can be refused if the app went to the background first; the queue still runs while the process lives
        runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type) }
    }

    private fun notification(active: List<DownloadEntity>, live: Map<Long, LiveProgress>): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Unduhan", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val current = active.firstOrNull { live.containsKey(it.id) } ?: active.firstOrNull()
        val progress = current?.let { live[it.id] }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val cancelAll = PendingIntent.getService(
            this, 1, Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_ALL),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (active.size > 1) t("Mengunduh ${active.size} lagu", "Downloading ${active.size} songs") else t("Mengunduh lagu", "Downloading song"))
            .setContentText(current?.title ?: t("Menyiapkan…", "Preparing…"))
            .setSubText(progress?.speed)
            .setProgress(100, ((progress?.fraction ?: 0f) * 100).toInt(), progress == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, t("Batalkan semua", "Cancel all"), cancelAll)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_CANCEL_ALL = "com.thecrudify.zdownload.CANCEL_ALL"

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            // Refused when started from the background (Android 12+); downloads then run without the notification
            runCatching {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
            }
        }
    }
}
