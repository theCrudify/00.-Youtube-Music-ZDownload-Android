package com.thecrudify.zdownload.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.thecrudify.zdownload.playback.PlaybackController
import com.thecrudify.zdownload.playback.PlayerState
import com.thecrudify.zdownload.ui.components.Artwork
import com.thecrudify.zdownload.ui.components.formatDuration
import com.thecrudify.zdownload.ui.tr
import kotlinx.coroutines.delay

/** Current playback position, polled only while this composable is on screen. */
@Composable
private fun rememberPosition(playback: PlaybackController, state: PlayerState): Long {
    var position by remember { mutableLongStateOf(playback.positionMs) }
    LaunchedEffect(state.isPlaying, state.index, state.current?.trackId) {
        position = playback.positionMs
        while (state.isPlaying) {
            delay(500)
            position = playback.positionMs
        }
    }
    return position
}

/** Average color of the cover art (bitmap scaled to 1x1), or null when there is no readable cover. */
@Composable
private fun rememberCoverColor(path: String?): Color? {
    val context = LocalContext.current
    val color by produceState<Color?>(null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(File(path)).size(32).allowHardware(false).build()
            (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
                ?.let { Color(Bitmap.createScaledBitmap(it, 1, 1, true).getPixel(0, 0)) }
        }
    }
    return color
}

/** Always-visible bar above the navigation: what's playing, play/pause, next, progress. Tap to open Now Playing. */
@Composable
fun MiniPlayer(playback: PlaybackController, state: PlayerState, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val current = state.current ?: return
    val position = rememberPosition(playback, state)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClickLabel = tr("Buka pemutar", "Open player"), onClick = onExpand)
    ) {
        LinearProgressIndicator(
            progress = { if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp)
        )
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Artwork(current.artworkPath, Modifier.size(48.dp), shape = MaterialTheme.shapes.small)
            Column(modifier = Modifier.weight(1f)) {
                Text(current.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(current.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = playback::togglePlay) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) tr("Jeda", "Pause") else tr("Putar", "Play")
                )
            }
            IconButton(onClick = playback::next) {
                Icon(Icons.Default.SkipNext, contentDescription = tr("Berikutnya", "Next"))
            }
        }
    }
}

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Full-screen Now Playing: artwork, timeline, transport, repeat/shuffle, speed, volume, lyrics, and the queue. */
@Composable
fun ExpandedPlayer(
    playback: PlaybackController,
    state: PlayerState,
    isFavorite: Boolean,
    lyrics: String? = null,
    onToggleFavorite: () -> Unit,
    onCollapse: () -> Unit,
    onEditLyrics: (() -> Unit)? = null
) {
    val current = state.current
    var activeTab by rememberSaveable { mutableStateOf("artwork") } // "artwork", "queue", "lyrics"
    // 0.25 keeps onSurfaceVariant text at ~4.5:1 even on near-white or near-black covers.
    val coverColor = rememberCoverColor(current?.artworkPath)
    val background by animateColorAsState(
        coverColor?.let { lerp(MaterialTheme.colorScheme.surface, it, 0.25f) } ?: MaterialTheme.colorScheme.surfaceContainer,
        label = "playerBackground"
    )

    // Drawn outside the Scaffold, so nothing else sets the default text/icon color here.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            // Swallow taps so they don't reach the screen underneath
            .pointerInput(Unit) { detectTapGestures { } }
            .systemBarsPadding()
    ) {
        val wide = maxWidth > maxHeight && maxWidth >= 600.dp
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = tr("Tutup pemutar", "Close player"))
                }
                Text(
                    when (activeTab) {
                        "queue" -> tr("Antrean", "Queue")
                        "lyrics" -> tr("Lirik Lagu", "Lyrics")
                        else -> tr("Sedang diputar", "Now playing")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { activeTab = if (activeTab == "lyrics") "artwork" else "lyrics" }) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = tr("Lirik", "Lyrics"),
                        tint = if (activeTab == "lyrics") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { activeTab = if (activeTab == "queue") "artwork" else "queue" }) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = tr("Antrean", "Queue"),
                        tint = if (activeTab == "queue") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (current == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tr("Belum ada lagu yang diputar", "Nothing is playing"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            if (wide) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        when (activeTab) {
                            "queue" -> QueueList(playback, state)
                            "lyrics" -> LyricsView(lyrics, onEditLyrics)
                            else -> Artwork(current.artworkPath, Modifier.fillMaxHeight(0.85f).aspectRatio(1f), shape = MaterialTheme.shapes.large)
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        Controls(playback, state, isFavorite, onToggleFavorite)
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                    when (activeTab) {
                        "queue" -> QueueList(playback, state)
                        "lyrics" -> LyricsView(lyrics, onEditLyrics)
                        else -> Artwork(current.artworkPath, Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f), shape = MaterialTheme.shapes.large)
                    }
                }
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Controls(playback, state, isFavorite, onToggleFavorite)
                }
            }
        }
    }
    }
}

@Composable
private fun Controls(playback: PlaybackController, state: PlayerState, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    val current = state.current ?: return
    val position = rememberPosition(playback, state)
    // While dragging, the thumb follows the finger rather than jumping back to the playing position
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val colors = MaterialTheme.colorScheme
    // On a cover-tinted background the default surfaceVariant track can vanish; outlineVariant stays visible.
    val sliderColors = SliderDefaults.colors(inactiveTrackColor = colors.outlineVariant)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(current.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(current.artist, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) tr("Hapus dari favorit", "Remove from favorites") else tr("Tambah ke favorit", "Add to favorites"),
                tint = if (isFavorite) colors.primary else colors.onSurface
            )
        }
    }

    Slider(
        value = if (dragging) dragValue else position.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
        valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
        onValueChange = { dragging = true; dragValue = it },
        onValueChangeFinished = { playback.seekTo(dragValue.toLong()); dragging = false },
        colors = sliderColors,
        modifier = Modifier.fillMaxWidth()
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDuration(if (dragging) dragValue.toLong() else position), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = playback::toggleShuffle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = if (state.shuffle) tr("Acak aktif", "Shuffle on") else tr("Acak mati", "Shuffle off"),
                tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant
            )
        }
        IconButton(onClick = playback::previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipPrevious, contentDescription = tr("Sebelumnya", "Previous"), modifier = Modifier.size(36.dp))
        }
        FilledIconButton(onClick = playback::togglePlay, modifier = Modifier.size(72.dp), shape = CircleShape) {
            Icon(
                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) tr("Jeda", "Pause") else tr("Putar", "Play"),
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = playback::next, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipNext, contentDescription = tr("Berikutnya", "Next"), modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = playback::cycleRepeat) {
            Icon(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = when (state.repeatMode) {
                    Player.REPEAT_MODE_ONE -> tr("Ulangi satu lagu", "Repeat one")
                    Player.REPEAT_MODE_ALL -> tr("Ulangi semua", "Repeat all")
                    else -> tr("Ulangi mati", "Repeat off")
                },
                tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) colors.onSurfaceVariant else colors.primary
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { playback.setSpeed(SPEEDS[(SPEEDS.indexOf(state.speed).coerceAtLeast(0) + 1) % SPEEDS.size]) }) {
            Text(
                "${state.speed.toString().removeSuffix(".0")}×",
                style = MaterialTheme.typography.labelLarge,
                color = if (state.speed == 1f) colors.onSurfaceVariant else colors.primary
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = playback::toggleMute) {
            Icon(
                if (state.volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (state.volume == 0f) tr("Bunyikan", "Unmute") else tr("Bisukan", "Mute"),
                tint = colors.onSurfaceVariant
            )
        }
        Slider(
            value = state.volume,
            onValueChange = playback::setVolume,
            colors = sliderColors,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * The play queue. Editing it changes only what plays next; playlists and the library are untouched.
 * shortcut: reorder is up/down buttons, not drag-and-drop (Compose has no built-in list drag); swap in a reorder library if drag is wanted.
 */
@Composable
private fun QueueList(playback: PlaybackController, state: PlayerState) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.index.coerceAtLeast(0))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("${state.queue.size} lagu di antrean", "${state.queue.size} songs in queue"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = playback::clearQueue) { Text(tr("Kosongkan", "Clear")) }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            // Index in the key: the same song can be queued twice
            itemsIndexed(state.queue, key = { i, item -> "$i-${item.trackId}" }) { i, item ->
                val isCurrent = i == state.index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { playback.playQueueItem(i) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(item.artworkPath, Modifier.size(40.dp), shape = MaterialTheme.shapes.extraSmall)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(item.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { playback.moveQueueItem(i, i - 1) }, enabled = i > 0, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = tr("Naikkan", "Move up"))
                    }
                    IconButton(onClick = { playback.moveQueueItem(i, i + 1) }, enabled = i < state.queue.lastIndex, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = tr("Turunkan", "Move down"))
                    }
                    IconButton(onClick = { playback.removeQueueItem(i) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Hapus dari antrean", "Remove from queue"))
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsView(lyrics: String?, onEditLyrics: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!lyrics.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = lyrics,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Text(tr("Lirik belum tersedia", "No lyrics available"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (onEditLyrics != null) {
                    TextButton(onClick = onEditLyrics) {
                        Text(tr("Tambah / Edit Lirik", "Add / Edit Lyrics"))
                    }
                }
            }
        }
    }
}
