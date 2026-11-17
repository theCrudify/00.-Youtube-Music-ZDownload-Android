package com.thecrudify.zdownload.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.thecrudify.zdownload.MainActivity

/**
 * Owns the one ExoPlayer for the whole app. Being a MediaSessionService gives background and
 * lock-screen playback, the media notification, and headset/Bluetooth buttons; the UI talks to it
 * through a MediaController (see PlaybackController) and never creates a player of its own.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            // Pause for calls / other apps' audio, and when headphones are unplugged
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            // Keep the CPU awake while playing with the screen off
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(object : MediaSession.Callback {
                // A controller's items can arrive without their playable URI; it travels in requestMetadata
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
                    mediaItems.map { item ->
                        if (item.localConfiguration != null) item
                        else item.buildUpon().setUri(item.requestMetadata.mediaUri).build()
                    }.toMutableList()
                )
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    // Swiping the app away stops the service unless music is actually playing
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
