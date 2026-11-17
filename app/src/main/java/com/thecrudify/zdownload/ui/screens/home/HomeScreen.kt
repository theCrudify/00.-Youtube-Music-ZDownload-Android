package com.thecrudify.zdownload.ui.screens.home

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.ui.components.Artwork
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    val allTracks by libraryVm.allTracks.collectAsState()
    val recent = remember(allTracks) { allTracks.sortedByDescending { it.entity.dateAdded }.take(12) }
    // The backdrop follows the music: the track being analyzed, else the newest song in the library.
    val backdrop: Any? = (state as? DownloadState.TrackLoaded)?.trackInfo?.thumbnailUrl
        ?: (state as? DownloadState.Downloading)?.trackInfo?.thumbnailUrl
        ?: (state as? DownloadState.Success)?.trackInfo?.thumbnailUrl
        ?: recent.firstOrNull()?.entity?.artworkPath?.let(::File)

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
                item {
                    Text(
                        greeting(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp).semantics { heading() }
                    )
                }
                item {
                    GlassCard {
                        LinkField(
                            url = url,
                            canSubmit = url.isNotBlank() && (state is DownloadState.Idle || state is DownloadState.Error),
                            onUrlChanged = viewModel::onUrlChanged,
                            onPaste = viewModel::pasteFromClipboard,
                            onClear = viewModel::clearUrl,
                            onAnalyze = viewModel::analyzeUrl
                        )
                    }
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
                if (recent.isNotEmpty()) {
                    item { RecentRow(recent, onPlay = { libraryVm.play(recent, it, shuffle = false) }) }
                } else if (state is DownloadState.Idle) {
                    item { FirstRun() }
                }
                if (downloads.isEmpty()) {
                    if (state is DownloadState.Idle && recent.isNotEmpty()) item { ShareHint() }
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

@Composable
private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 4..10 -> tr("Selamat pagi", "Good morning")
    in 11..14 -> tr("Selamat siang", "Good afternoon")
    in 15..17 -> tr("Selamat sore", "Good evening")
    else -> tr("Selamat malam", "Good night")
}

/** Blurred cover behind the top of the screen, fading into the surface; theme-colored glows when there is no cover. */
@Composable
private fun HeroBackdrop(model: Any?) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
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
                drawCircle(Brush.radialGradient(listOf(primary.copy(alpha = 0.28f), Color.Transparent), center = Offset(size.width * 0.15f, size.height * 0.1f), radius = size.width * 0.8f))
                drawCircle(Brush.radialGradient(listOf(tertiary.copy(alpha = 0.2f), Color.Transparent), center = Offset(size.width * 0.95f, size.height * 0.35f), radius = size.width * 0.7f))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.2f), surface.copy(alpha = 0.6f), surface))))
    }
}

/** Frosted panel: translucent surface + hairline border, so the blurred cover shows through. */
@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) { Box(Modifier.padding(vertical = 8.dp)) { content() } }
}

@Composable
private fun RecentRow(tracks: List<Track>, onPlay: (Int) -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            tr("Baru ditambahkan", "Recently added"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                Column(
                    Modifier.width(140.dp).clip(MaterialTheme.shapes.medium).clickable(onClickLabel = tr("Putar", "Play")) { onPlay(index) }
                ) {
                    Artwork(track.entity.artworkPath, Modifier.size(140.dp), shape = MaterialTheme.shapes.medium)
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(track.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** First launch, empty library: a vinyl drawn in theme colors instead of a blank screen. */
@Composable
private fun FirstRun() {
    val groove = MaterialTheme.colorScheme.onSurface
    val label = MaterialTheme.colorScheme.primary
    val disc = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(Modifier.fillMaxWidth().padding(top = 40.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(180.dp)) {
            val r = size.minDimension / 2
            drawCircle(disc, r)
            for (i in 1..6) drawCircle(groove.copy(alpha = 0.07f), r * (0.45f + i * 0.085f), style = Stroke(1.dp.toPx()))
            drawCircle(label, r * 0.32f)
            drawCircle(disc, r * 0.05f)
        }
        Text(
            tr("Koleksimu masih kosong", "Your collection is empty"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            tr(
                "Tempel link di atas, atau tekan Bagikan di aplikasi YouTube / YouTube Music lalu pilih ZDownload.",
                "Paste a link above, or tap Share in YouTube / YouTube Music and pick ZDownload."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
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
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(tr("Link YouTube atau YouTube Music", "YouTube or YouTube Music link")) },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            trailingIcon = {
                if (url.isEmpty()) {
                    IconButton(onClick = onPaste) { Icon(Icons.Default.ContentPaste, tr("Tempel", "Paste")) }
                } else {
                    IconButton(onClick = onClear) { Icon(Icons.Default.Close, tr("Hapus link", "Clear link")) }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (canSubmit) onAnalyze() }),
            // The glass card is the visual container; the border only appears while typing.
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent)
        )
        if (canSubmit) {
            Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) { Text(tr("Cari lagu", "Find track")) }
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
    val pad = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    when (state) {
        is DownloadState.Idle -> Unit
        is DownloadState.FetchingInfo -> Column(pad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                tr("Mengambil info lagu…", "Fetching track info…"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is DownloadState.Error -> Surface(pad, color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        is DownloadState.TrackLoaded -> TrackCard(state.trackInfo, pad) {
            FormatPicker(selectedFormat, onFormatSelected)
            DownloadButton { onDownload(state.trackInfo) }
        }
        is DownloadState.Downloading -> TrackCard(state.trackInfo, pad) {
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            Text(state.statusMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is DownloadState.Success -> TrackCard(state.trackInfo, pad) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(tr("Tersimpan di library", "Saved to library"), style = MaterialTheme.typography.titleSmall)
                    Text(
                        state.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                state.trackId?.let { id ->
                    FilledTonalButton(onClick = { onPlay(id) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Putar", "Play"))
                    }
                }
            }
            // Picking another format here downloads a second copy in that format.
            FormatPicker(selectedFormat, onFormatSelected)
            DownloadButton { onDownload(state.trackInfo) }
        }
    }
}

@Composable
private fun TrackCard(info: TrackInfo, modifier: Modifier, content: @Composable () -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = info.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(info.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${info.uploader} · ${info.formattedDuration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatPicker(selected: AudioFormat, onSelect: (AudioFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AudioFormat.entries.forEach { format ->
                FilterChip(selected = format == selected, onClick = { onSelect(format) }, label = { Text(format.title) })
            }
        }
        Text(
            "${format(selected).first} · ${format(selected).second}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun DownloadButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(tr("Unduh", "Download"))
    }
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
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        headlineContent = { Text(d.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${d.artist} · ${d.format}", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
