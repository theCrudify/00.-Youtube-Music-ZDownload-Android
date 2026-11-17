package com.thecrudify.zdownload.ui.player

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.thecrudify.zdownload.playback.PlaybackController
import com.thecrudify.zdownload.playback.PlayerState
import com.thecrudify.zdownload.ui.components.Artwork
import com.thecrudify.zdownload.ui.components.formatDuration
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.BgBase
import com.thecrudify.zdownload.ui.theme.BgElevated
import com.thecrudify.zdownload.ui.theme.BorderSubtle
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary
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

/** Always-visible bar above the navigation: what's playing, play/pause, next, progress. Tap to open Now Playing. */
@Composable
fun MiniPlayer(playback: PlaybackController, state: PlayerState, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val current = state.current ?: return
    val position = rememberPosition(playback, state)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgElevated)
            .clickable(onClickLabel = tr("Buka pemutar", "Open player"), onClick = onExpand)
    ) {
        LinearProgressIndicator(
            progress = { if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = AccentCyan,
            trackColor = BorderSubtle
        )
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Artwork(current.artworkPath, Modifier.size(44.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(current.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(current.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = playback::togglePlay) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) tr("Jeda", "Pause") else tr("Putar", "Play"),
                    tint = TextPrimary, modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = playback::next) {
                Icon(Icons.Default.SkipNext, contentDescription = tr("Berikutnya", "Next"), tint = TextPrimary)
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
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
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = tr("Tutup pemutar", "Close player"), tint = TextPrimary)
                }
                Text(
                    when (activeTab) {
                        "queue" -> tr("Antrean", "Queue")
                        "lyrics" -> tr("Lirik Lagu", "Lyrics")
                        else -> tr("Sedang diputar", "Now playing")
                    },
                    color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { activeTab = if (activeTab == "lyrics") "artwork" else "lyrics" }) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = tr("Lirik", "Lyrics"),
                        tint = if (activeTab == "lyrics") AccentCyan else TextPrimary
                    )
                }
                IconButton(onClick = { activeTab = if (activeTab == "queue") "artwork" else "queue" }) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = tr("Antrean", "Queue"),
                        tint = if (activeTab == "queue") AccentCyan else TextPrimary
                    )
                }
            }

            if (current == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tr("Belum ada lagu yang diputar", "Nothing is playing"), color = TextSecondary)
                }
                return@Column
            }

            if (wide) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        when (activeTab) {
                            "queue" -> QueueList(playback, state)
                            "lyrics" -> LyricsView(lyrics, onEditLyrics)
                            else -> Artwork(current.artworkPath, Modifier.fillMaxHeight(0.85f).aspectRatio(1f), corner = 20.dp)
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
                        else -> Artwork(current.artworkPath, Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f), corner = 20.dp)
                    }
                }
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Controls(playback, state, isFavorite, onToggleFavorite)
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
    val sliderColors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan, inactiveTrackColor = BorderSubtle)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(current.title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(current.artist, color = TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) tr("Hapus dari favorit", "Remove from favorites") else tr("Tambah ke favorit", "Add to favorites"),
                tint = if (isFavorite) Color(0xFFF43F5E) else TextPrimary
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
        Text(formatDuration(if (dragging) dragValue.toLong() else position), color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(formatDuration(state.durationMs), color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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
                tint = if (state.shuffle) AccentCyan else TextMuted
            )
        }
        IconButton(onClick = playback::previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipPrevious, contentDescription = tr("Sebelumnya", "Previous"), tint = TextPrimary, modifier = Modifier.size(36.dp))
        }
        IconButton(
            onClick = playback::togglePlay,
            modifier = Modifier.size(68.dp).clip(CircleShape).background(AccentCyan)
        ) {
            Icon(
                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) tr("Jeda", "Pause") else tr("Putar", "Play"),
                tint = BgBase, modifier = Modifier.size(38.dp)
            )
        }
        IconButton(onClick = playback::next, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipNext, contentDescription = tr("Berikutnya", "Next"), tint = TextPrimary, modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = playback::cycleRepeat) {
            Icon(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = when (state.repeatMode) {
                    Player.REPEAT_MODE_ONE -> tr("Ulangi satu lagu", "Repeat one")
                    Player.REPEAT_MODE_ALL -> tr("Ulangi semua", "Repeat all")
                    else -> tr("Ulangi mati", "Repeat off")
                },
                tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) TextMuted else AccentCyan
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { playback.setSpeed(SPEEDS[(SPEEDS.indexOf(state.speed).coerceAtLeast(0) + 1) % SPEEDS.size]) }) {
            Text(
                "${state.speed.toString().removeSuffix(".0")}×",
                color = if (state.speed == 1f) TextSecondary else AccentCyan,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = playback::toggleMute) {
            Icon(
                if (state.volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (state.volume == 0f) tr("Bunyikan", "Unmute") else tr("Bisukan", "Mute"),
                tint = TextSecondary
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
                color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f)
            )
            TextButton(onClick = playback::clearQueue) { Text(tr("Kosongkan", "Clear"), color = AccentCyan) }
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
                    Artwork(item.artworkPath, Modifier.size(40.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(item.title, color = if (isCurrent) AccentCyan else TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artist, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { playback.moveQueueItem(i, i - 1) }, enabled = i > 0, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = tr("Naikkan", "Move up"), tint = if (i > 0) TextSecondary else BorderSubtle)
                    }
                    IconButton(onClick = { playback.moveQueueItem(i, i + 1) }, enabled = i < state.queue.lastIndex, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = tr("Turunkan", "Move down"), tint = if (i < state.queue.lastIndex) TextSecondary else BorderSubtle)
                    }
                    IconButton(onClick = { playback.removeQueueItem(i) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Hapus dari antrean", "Remove from queue"), tint = TextSecondary)
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
                    color = TextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Description, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Text(tr("Lirik belum tersedia", "No lyrics available"), color = TextSecondary, fontSize = 15.sp)
                if (onEditLyrics != null) {
                    TextButton(onClick = onEditLyrics) {
                        Text(tr("Tambah / Edit Lirik", "Add / Edit Lyrics"), color = AccentCyan)
                    }
                }
            }
        }
    }
}
