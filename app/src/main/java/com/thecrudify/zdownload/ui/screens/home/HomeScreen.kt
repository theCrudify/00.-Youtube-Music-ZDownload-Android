package com.thecrudify.zdownload.ui.screens.home

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.remember
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import com.thecrudify.zdownload.ui.theme.MascotBlue
import com.thecrudify.zdownload.ui.theme.MascotSky
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thecrudify.zdownload.R
import com.thecrudify.zdownload.data.db.DownloadStatus
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.model.DownloadState
import com.thecrudify.zdownload.data.model.TrackInfo
import com.thecrudify.zdownload.ui.components.formatBytes
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.DownloadItem
import com.thecrudify.zdownload.viewmodel.LibraryViewModel
import com.thecrudify.zdownload.viewmodel.MainViewModel

/** The Unduh tab: paste a link, pick a format, and watch the download queue below. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, libraryVm: LibraryViewModel, onOpenSettings: () -> Unit) {
    val url by viewModel.url.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val state by viewModel.downloadState.collectAsState()
    val downloads by libraryVm.downloads.collectAsState()
    // Before a track is picked the screen greets; afterwards the track takes the space.
    val showGreeting = state is DownloadState.Idle || state is DownloadState.FetchingInfo || state is DownloadState.Error
    val backdrop: String? = when (val st = state) {
        is DownloadState.TrackLoaded -> st.trackInfo.thumbnailUrl
        is DownloadState.Downloading -> st.trackInfo.thumbnailUrl
        is DownloadState.Success -> st.trackInfo.thumbnailUrl
        else -> null
    }

    Box(Modifier.fillMaxSize()) {
        HeroBackdrop(backdrop)
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                }
            )
            // Content width capped on tablets so rows stay readable.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (showGreeting) item { Greeting() }
                    item {
                        LinkField(
                            url = url,
                            canSubmit = url.isNotBlank() && (state is DownloadState.Idle || state is DownloadState.Error),
                            onUrlChanged = viewModel::onUrlChanged,
                            onPaste = viewModel::pasteFromClipboard,
                            onClear = viewModel::clearUrl,
                            onAnalyze = viewModel::analyzeUrl
                        )
                    }
                    item {
                        StateContent(
                            state = state,
                            selectedFormat = selectedFormat,
                            onFormatSelected = viewModel::selectFormat,
                            onDownload = viewModel::startDownload,
                            onPlay = libraryVm::playDownloaded
                        )
                    }
                    if (downloads.isEmpty()) {
                        if (state is DownloadState.Idle) item { ShareHint() }
                    } else {
                        item {
                            QueueHeader(
                                canClear = downloads.any { !it.download.statusEnum.isActive && it.download.statusEnum != DownloadStatus.PAUSED },
                                onClear = libraryVm::clearFinishedDownloads
                            )
                        }
                        items(downloads, key = { it.download.id }) { DownloadRow(it, libraryVm) }
                    }
                }
            }
        }
    }
}

/** Mascot + time-of-day greeting; the mascot bobs gently (the system animation scale turns this off). */
@Composable
private fun Greeting() {
    val bob by rememberInfiniteTransition(label = "mascot").animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painterResource(R.drawable.mascot),
            contentDescription = null,
            modifier = Modifier.size(168.dp).graphicsLayer { translationY = bob.dp.toPx() }
        )
        Text(
            "${greeting()} 👋",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() }
        )
        Text(
            tr("Mau dengerin lagu apa hari ini?", "What are you listening to today?"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 4..10 -> tr("Selamat pagi", "Good morning")
    in 11..14 -> tr("Selamat siang", "Good afternoon")
    in 15..17 -> tr("Selamat sore", "Good evening")
    else -> tr("Selamat malam", "Good night")
}

/** Blurred cover behind the top of the screen, fading into the surface; blue glows (the mascot's colors) when there is none. */
@Composable
private fun HeroBackdrop(model: String?) {
    val surface = MaterialTheme.colorScheme.surface
    Box(Modifier.fillMaxWidth().height(420.dp)) {
        if (model != null) {
            // Modifier.blur only renders on Android 12+; below that the heavier scrim keeps text readable.
            val strongBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Crossfade(model, label = "backdrop") { m ->
                AsyncImage(
                    model = m,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (strongBlur) 0.7f else 0.35f,
                    modifier = Modifier.fillMaxSize().blur(60.dp)
                )
            }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(MascotBlue.copy(alpha = 0.30f), Color.Transparent), center = Offset(size.width * 0.15f, size.height * 0.1f), radius = size.width * 0.8f))
                drawRect(Brush.radialGradient(listOf(MascotSky.copy(alpha = 0.24f), Color.Transparent), center = Offset(size.width * 0.95f, size.height * 0.35f), radius = size.width * 0.7f))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.2f), surface.copy(alpha = 0.6f), surface))))
    }
}

@Composable
private fun LinkField(
    url: String,
    canSubmit: Boolean,
    onUrlChanged: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onAnalyze: () -> Unit
) {
    val focus = remember { MutableInteractionSource() }
    val focused by focus.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val outline by animateColorAsState(if (focused) scheme.primary else scheme.onSurface.copy(alpha = 0.12f), label = "linkOutline")
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // One frosted pill: no inner field box; focus only recolors the pill's outline.
        Surface(
            shape = CircleShape,
            color = scheme.surfaceContainerHigh.copy(alpha = 0.7f),
            border = BorderStroke(if (focused) 2.dp else 1.dp, outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = url,
                onValueChange = onUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = focus,
                placeholder = { Text(tr("Link YouTube atau YouTube Music", "YouTube or YouTube Music link"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.padding(start = 8.dp)) },
                trailingIcon = {
                    Box(Modifier.padding(end = 4.dp)) {
                        if (url.isEmpty()) {
                            IconButton(onClick = onPaste) { Icon(Icons.Default.ContentPaste, tr("Tempel", "Paste")) }
                        } else {
                            IconButton(onClick = onClear) { Icon(Icons.Default.Close, tr("Hapus link", "Clear link")) }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (canSubmit) onAnalyze() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
        if (canSubmit) {
            Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(tr("Cari lagu", "Find track"), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun StateContent(
    state: DownloadState,
    selectedFormat: AudioFormat,
    onFormatSelected: (AudioFormat) -> Unit,
    onDownload: (TrackInfo) -> Unit,
    onPlay: (String) -> Unit
) {
    val pad = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    when (state) {
        is DownloadState.Idle -> Unit
        is DownloadState.FetchingInfo -> Row(
            pad.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                tr("Mengambil info lagu…", "Fetching track info…"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is DownloadState.Error -> Surface(pad, color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        is DownloadState.TrackLoaded -> TrackHero(state.trackInfo, pad) {
            FormatPicker(selectedFormat, onFormatSelected)
            Button(onClick = { onDownload(state.trackInfo) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr("Unduh ${selectedFormat.title}", "Download ${selectedFormat.title}"), style = MaterialTheme.typography.titleMedium)
            }
        }
        is DownloadState.Downloading -> {
            val progress by animateFloatAsState(state.progress, label = "progress")
            TrackHero(state.trackInfo, pad, overlay = {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(84.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        strokeWidth = 6.dp
                    )
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }) {
                Text(state.statusMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        is DownloadState.Success -> TrackHero(state.trackInfo, pad, overlay = {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
            }
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("Tersimpan di library", "Saved to library"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(state.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            state.trackId?.let { id ->
                Button(onClick = { onPlay(id) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Putar sekarang", "Play now"), style = MaterialTheme.typography.titleMedium)
                }
            }
            // Picking another format here downloads a second copy in that format.
            FormatPicker(selectedFormat, onFormatSelected)
            OutlinedButton(onClick = { onDownload(state.trackInfo) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Unduh juga sebagai ${selectedFormat.title}", "Also download as ${selectedFormat.title}"))
            }
        }
    }
}

/** Glass card led by the video thumbnail at full width, with an optional overlay for progress/done. */
@Composable
private fun TrackHero(
    info: TrackInfo,
    modifier: Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.large)) {
                AsyncImage(
                    model = info.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Photo overlay: fixed black/white reads on any thumbnail, regardless of theme.
                Surface(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(info.formattedDuration, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                overlay()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(info.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(info.uploader, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            content()
        }
    }
}

/** Formats as a 2-column grid of selectable tiles: name, quality badge, and a check on the chosen one. */
@Composable
private fun FormatPicker(selected: AudioFormat, onSelect: (AudioFormat) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr("Pilih format", "Choose format"), style = MaterialTheme.typography.titleSmall)
        AudioFormat.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { f -> FormatTile(f, f == selected, { onSelect(f) }, Modifier.weight(1f)) }
            }
        }
        Text(format(selected).second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormatTile(f: AudioFormat, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.RadioButton },
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.6f),
        contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
        border = if (isSelected) BorderStroke(2.dp, scheme.primary) else null
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(format(f).first, style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f), maxLines = 1)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Localized badge + description for a format. */
@Composable
private fun format(f: AudioFormat): Pair<String, String> = when (f) {
    AudioFormat.ORIGINAL -> stringResource(R.string.format_original_badge) to stringResource(R.string.format_original_desc)
    AudioFormat.MP3 -> stringResource(R.string.format_mp3_badge) to stringResource(R.string.format_mp3_desc)
    AudioFormat.M4A -> stringResource(R.string.format_m4a_badge) to stringResource(R.string.format_m4a_desc)
    AudioFormat.FLAC -> stringResource(R.string.format_flac_badge) to stringResource(R.string.format_flac_desc)
    AudioFormat.WAV -> stringResource(R.string.format_wav_badge) to stringResource(R.string.format_wav_desc)
    AudioFormat.OPUS -> stringResource(R.string.format_opus_badge) to stringResource(R.string.format_opus_desc)
}

@Composable
private fun ShareHint() {
    Text(
        tr(
            "Bisa juga lewat tombol Bagikan di aplikasi YouTube atau YouTube Music, lalu pilih ZDownload.",
            "You can also tap Share in the YouTube or YouTube Music app and pick ZDownload."
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun QueueHeader(canClear: Boolean, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            tr("Unduhan", "Downloads"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        TextButton(onClick = onClear, enabled = canClear) { Text(tr("Bersihkan", "Clear")) }
    }
}

@Composable
private fun statusLabel(status: DownloadStatus) = when (status) {
    DownloadStatus.PENDING -> tr("Menunggu giliran", "Waiting in queue")
    DownloadStatus.DOWNLOADING -> tr("Mengunduh", "Downloading")
    DownloadStatus.VERIFYING -> tr("Memeriksa file", "Verifying file")
    DownloadStatus.EXTRACTING_METADATA -> tr("Membaca metadata", "Reading metadata")
    DownloadStatus.REGISTERING_LIBRARY -> tr("Menyimpan ke library", "Adding to library")
    DownloadStatus.COMPLETED -> tr("Selesai", "Completed")
    DownloadStatus.FAILED -> tr("Gagal", "Failed")
    DownloadStatus.CANCELLED -> tr("Dibatalkan", "Cancelled")
    DownloadStatus.PAUSED -> tr("Dijeda", "Paused")
}

@Composable
private fun DownloadRow(item: DownloadItem, vm: LibraryViewModel) {
    val d = item.download
    val status = d.statusEnum
    val progress = item.progress
    val detail = when {
        status == DownloadStatus.DOWNLOADING && progress != null -> listOfNotNull(
            "${(progress.fraction * 100).toInt()}%",
            progress.speed,
            progress.etaSeconds?.let { tr("sisa $it dtk", "${it}s left") }
        ).joinToString(" · ")
        status == DownloadStatus.COMPLETED -> d.fileSize?.let(::formatBytes).orEmpty()
        status == DownloadStatus.FAILED -> d.error.orEmpty()
        else -> ""
    }
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            AsyncImage(
                model = d.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small)
            )
        },
        headlineContent = { Text(d.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Format only matters while choosing/downloading; a finished song is just a song.
                Text(if (status == DownloadStatus.COMPLETED) d.artist else "${d.artist} · ${d.format}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (status.isActive) {
                    // No measured progress for this stage: show activity, not a made-up percentage.
                    if (status == DownloadStatus.DOWNLOADING && progress != null) {
                        LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    } else {
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                }
                Text(
                    listOf(statusLabel(status), detail).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = {
            Row {
                when {
                    status.isActive -> {
                        // Pausing kills yt-dlp but keeps the partial file; a pending item has nothing to pause yet.
                        if (status == DownloadStatus.DOWNLOADING) {
                            IconButton(onClick = { vm.pauseDownload(d.id) }) { Icon(Icons.Default.Pause, tr("Jeda", "Pause")) }
                        }
                        IconButton(onClick = { vm.cancelDownload(d.id) }) { Icon(Icons.Default.Close, tr("Batalkan", "Cancel")) }
                    }
                    status == DownloadStatus.COMPLETED -> {
                        d.trackId?.let { id -> IconButton(onClick = { vm.playDownloaded(id) }) { Icon(Icons.Default.PlayArrow, tr("Putar", "Play")) } }
                        IconButton(onClick = { vm.removeDownload(d.id) }) { Icon(Icons.Default.Close, tr("Hapus dari riwayat", "Remove from history")) }
                    }
                    else -> {
                        IconButton(onClick = { vm.retryDownload(d.id) }) {
                            Icon(Icons.Default.Refresh, if (status == DownloadStatus.PAUSED) tr("Lanjutkan", "Resume") else tr("Ulangi", "Retry"))
                        }
                        IconButton(onClick = { vm.removeDownload(d.id) }) { Icon(Icons.Default.Close, tr("Hapus dari riwayat", "Remove from history")) }
                    }
                }
            }
        }
    )
}
