package com.thecrudify.zdownload.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.prefs.AppPreferences
import com.thecrudify.zdownload.data.prefs.PlaybackSnapshot
import com.thecrudify.zdownload.data.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class QueueItem(val trackId: String, val title: String, val artist: String, val artworkPath: String?)

data class PlayerState(
    val queue: List<QueueItem> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val speed: Float = 1f,
    val volume: Float = 1f
) {
    val current: QueueItem? get() = queue.getOrNull(index)
}

/**
 * The app's single entry point to playback. Wraps a MediaController bound to PlaybackService, exposes
 * what the UI needs as a StateFlow, and saves/restores the session (queue, position, modes).
 *
 * The queue here is the player's own list: editing it never touches a playlist or the library order.
 * All methods must be called on the main thread (MediaController requirement).
 */
class PlaybackController(
    private val context: Context,
    private val library: LibraryRepository,
    private val prefs: AppPreferences,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors

    private var controller: MediaController? = null
    private var volumeBeforeMute = 1f
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()

    val positionMs: Long get() = controller?.currentPosition?.coerceAtLeast(0) ?: 0

    fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            action(c)
        } else {
            pendingActions.add(action)
            connect()
        }
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            val actions = pendingActions.toList()
            pendingActions.clear()
            actions.forEach { it(c) }
            scope.launch {
                if (c.mediaItemCount == 0) restore(c)
                publish()
                // Position isn't an event; save it periodically so a killed process resumes close to where it was
                while (true) {
                    delay(10_000)
                    if (c.isPlaying) save()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish()
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED, Player.EVENT_VOLUME_CHANGED
                )
            ) save()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: return
            // Counts a play when a song actually starts (including each repeat), not when a queue is merely loaded
            if (controller?.playWhenReady == true) scope.launch { library.recordPlay(id) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            val failed = c.currentMediaItem
            val title = failed?.mediaMetadata?.title ?: "Lagu"
            val fileGone = failed?.requestMetadata?.mediaUri?.path?.let { !File(it).isFile } == true
            if (fileGone) failed?.mediaId?.let { id -> scope.launch { library.markMissing(id) } }
            _errors.tryEmit(if (fileGone) "File \"$title\" tidak ditemukan di perangkat" else "\"$title\" tidak bisa diputar")
            // Skip the broken item instead of leaving the player stuck on it
            val index = c.currentMediaItemIndex
            if (c.mediaItemCount > 1) {
                c.removeMediaItem(index)
                c.prepare()
                c.play()
            } else {
                c.clearMediaItems()
            }
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val uri = Uri.fromFile(File(entity.filePath))
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artistName)
                    .setAlbumTitle(albumTitle)
                    .setArtworkUri(entity.artworkPath?.let { Uri.fromFile(File(it)) })
                    .build()
            )
            .build()
    }

    private fun publish() {
        val c = controller ?: return
        val queue = (0 until c.mediaItemCount).map { i ->
            val item = c.getMediaItemAt(i)
            QueueItem(
                trackId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkPath = item.mediaMetadata.artworkUri?.path
            )
        }
        _state.value = PlayerState(
            queue = queue,
            index = if (queue.isEmpty()) -1 else c.currentMediaItemIndex,
            isPlaying = c.isPlaying,
            durationMs = c.duration.takeIf { it > 0 } ?: 0,
            repeatMode = c.repeatMode,
            shuffle = c.shuffleModeEnabled,
            speed = c.playbackParameters.speed,
            volume = c.volume
        )
    }

    private fun save() {
        val c = controller ?: return
        val snapshot = PlaybackSnapshot(
            queue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId },
            index = c.currentMediaItemIndex,
            positionMs = c.currentPosition.coerceAtLeast(0),
            repeatMode = c.repeatMode,
            shuffle = c.shuffleModeEnabled,
            speed = c.playbackParameters.speed,
            volume = c.volume
        )
        scope.launch { prefs.savePlaybackSnapshot(snapshot) }
    }

    /** Reloads the last queue, paused at the saved position. Tracks deleted since then are simply left out. */
    private suspend fun restore(c: MediaController) {
        val snapshot = prefs.playbackSnapshot()
        c.repeatMode = snapshot.repeatMode
        c.shuffleModeEnabled = snapshot.shuffle
        c.setPlaybackSpeed(snapshot.speed)
        c.volume = snapshot.volume
        if (snapshot.queue.isEmpty()) return
        val tracks = library.getTracks(snapshot.queue).filter { !it.entity.isMissing }
        if (tracks.isEmpty()) return
        val currentId = snapshot.queue.getOrNull(snapshot.index)
        val index = tracks.indexOfFirst { it.id == currentId }
        c.setMediaItems(
            tracks.map { it.toMediaItem() },
            index.coerceAtLeast(0),
            if (index >= 0) snapshot.positionMs else 0
        )
        c.prepare()
    }

    // ---- Queue ---------------------------------------------------------------------------

    /** Replaces the queue with [tracks] and starts at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean? = null) = withController { c ->
        val playable = tracks.filter { !it.entity.isMissing }
        if (playable.isEmpty()) {
            _errors.tryEmit("File lagu tidak ditemukan di perangkat")
            return@withController
        }
        val startId = tracks.getOrNull(startIndex)?.id
        val index = playable.indexOfFirst { it.id == startId }.coerceAtLeast(0)
        if (shuffle != null) c.shuffleModeEnabled = shuffle
        c.setMediaItems(playable.map { it.toMediaItem() }, index, 0)
        c.prepare()
        c.play()
    }

    fun playNext(tracks: List<Track>) = withController { c ->
        if (c.mediaItemCount == 0) return@withController play(tracks)
        c.addMediaItems(c.currentMediaItemIndex + 1, tracks.filter { !it.entity.isMissing }.map { it.toMediaItem() })
    }

    fun addToQueue(tracks: List<Track>) = withController { c ->
        if (c.mediaItemCount == 0) return@withController play(tracks)
        c.addMediaItems(tracks.filter { !it.entity.isMissing }.map { it.toMediaItem() })
    }

    fun playQueueItem(index: Int) = withController { c ->
        c.seekToDefaultPosition(index)
        c.prepare()
        c.play()
    }

    fun moveQueueItem(from: Int, to: Int) = withController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    fun removeQueueItem(index: Int) = withController { c ->
        c.removeMediaItem(index)
    }

    fun clearQueue() = withController { c ->
        c.clearMediaItems()
    }

    /** Call after tracks are deleted from the library so the queue doesn't hold dead entries. */
    fun removeTracks(trackIds: Set<String>) = withController { c ->
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId in trackIds) c.removeMediaItem(i)
        }
    }

    /** Call after a metadata edit so the queue, mini player and notification show the new info. */
    fun refreshTrack(track: Track) = withController { c ->
        for (i in 0 until c.mediaItemCount) {
            if (c.getMediaItemAt(i).mediaId == track.id) c.replaceMediaItem(i, track.toMediaItem())
        }
    }

    // ---- Transport -----------------------------------------------------------------------

    fun togglePlay() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    fun next() = withController { c ->
        c.seekToNext()
    }

    /** Restarts the song if it has played a few seconds, otherwise goes to the previous one (standard player behaviour). */
    fun previous() = withController { c ->
        c.seekToPrevious()
    }

    fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs)
    }

    /** off → all → one → off */
    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() = withController { c ->
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun setSpeed(speed: Float) = withController { c ->
        c.setPlaybackSpeed(speed)
    }

    fun setVolume(volume: Float) = withController { c ->
        c.volume = volume.coerceIn(0f, 1f)
    }

    fun toggleMute() = withController { c ->
        if (c.volume > 0f) {
            volumeBeforeMute = c.volume
            c.volume = 0f
        } else {
            c.volume = volumeBeforeMute.takeIf { it > 0f } ?: 1f
        }
    }
}
