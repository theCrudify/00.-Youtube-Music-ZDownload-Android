package com.thecrudify.zdownload.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecrudify.zdownload.data.db.DownloadStatus
import com.thecrudify.zdownload.ui.components.EmptyState
import com.thecrudify.zdownload.ui.components.ScreenHeader
import com.thecrudify.zdownload.ui.components.formatBytes
import com.thecrudify.zdownload.ui.theme.AccentAmber
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.AccentEmerald
import com.thecrudify.zdownload.ui.theme.BorderSubtle
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.DownloadItem
import com.thecrudify.zdownload.viewmodel.LibraryViewModel

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
fun DownloadsScreen(vm: LibraryViewModel) {
    val downloads by vm.downloads.collectAsState()
    val active = downloads.count { it.download.statusEnum.isActive }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(
            tr("Unduhan", "Downloads"),
            if (active > 0) tr("$active sedang berjalan", "$active in progress") else tr("${downloads.size} riwayat", "${downloads.size} in history")
        ) {
            IconButton(onClick = vm::clearFinishedDownloads, enabled = downloads.any { !it.download.statusEnum.isActive && it.download.statusEnum != DownloadStatus.PAUSED }) {
                Icon(Icons.Default.DeleteSweep, tr("Bersihkan riwayat", "Clear history"), tint = TextPrimary)
            }
        }
        if (downloads.isEmpty()) {
            EmptyState(
                Icons.Default.Download,
                tr("Belum ada unduhan", "No downloads yet"),
                tr("Tempel link YouTube atau YouTube Music di tab Beranda untuk mulai.", "Paste a YouTube or YouTube Music link on the Home tab to start.")
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(downloads, key = { it.download.id }) { item -> DownloadRow(item, vm) }
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, vm: LibraryViewModel) {
    val d = item.download
    val status = d.statusEnum
    val statusColor = when (status) {
        DownloadStatus.COMPLETED -> AccentEmerald
        DownloadStatus.FAILED -> Color(0xFFF87171)
        DownloadStatus.CANCELLED, DownloadStatus.PAUSED -> AccentAmber
        else -> AccentCyan
    }
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${d.artist} • ${d.format}", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                status.isActive -> {
                    // Pausing kills yt-dlp but keeps the partial file; a pending item has nothing to pause yet
                    if (status == DownloadStatus.DOWNLOADING) {
                        IconButton(onClick = { vm.pauseDownload(d.id) }) { Icon(Icons.Default.Pause, tr("Jeda", "Pause"), tint = TextPrimary) }
                    }
                    IconButton(onClick = { vm.cancelDownload(d.id) }) { Icon(Icons.Default.Close, tr("Batalkan", "Cancel"), tint = TextPrimary) }
                }
                status == DownloadStatus.COMPLETED -> {
                    d.trackId?.let { id -> IconButton(onClick = { vm.playDownloaded(id) }) { Icon(Icons.Default.PlayArrow, tr("Putar", "Play"), tint = AccentCyan) } }
                    IconButton(onClick = { vm.removeDownload(d.id) }) { Icon(Icons.Default.Close, tr("Hapus dari riwayat", "Remove from history"), tint = TextMuted) }
                }
                else -> {
                    IconButton(onClick = { vm.retryDownload(d.id) }) {
                        Icon(Icons.Default.Refresh, if (status == DownloadStatus.PAUSED) tr("Lanjutkan", "Resume") else tr("Ulangi", "Retry"), tint = AccentCyan)
                    }
                    IconButton(onClick = { vm.removeDownload(d.id) }) { Icon(Icons.Default.Close, tr("Hapus dari riwayat", "Remove from history"), tint = TextMuted) }
                }
            }
        }
        if (status.isActive) {
            val progress = item.progress
            if (status == DownloadStatus.DOWNLOADING && progress != null) {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp, top = 6.dp).height(4.dp),
                    color = AccentCyan, trackColor = BorderSubtle
                )
            } else {
                // No measured progress for this stage: show activity, not a made-up percentage
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp, top = 6.dp).height(4.dp),
                    color = AccentCyan, trackColor = BorderSubtle
                )
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(statusLabel(status), color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            val detail = when {
                status == DownloadStatus.DOWNLOADING && item.progress != null -> listOfNotNull(
                    "${(item.progress.fraction * 100).toInt()}%",
                    item.progress.speed,
                    item.progress.etaSeconds?.let { tr("sisa ${it} dtk", "${it}s left") }
                ).joinToString(" • ")
                status == DownloadStatus.COMPLETED -> d.fileSize?.let(::formatBytes).orEmpty()
                else -> ""
            }
            if (detail.isNotEmpty()) Text(detail, color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        if (status == DownloadStatus.FAILED && !d.error.isNullOrBlank()) {
            Text(d.error, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, end = 12.dp).size(width = 600.dp, height = androidx.compose.ui.unit.Dp.Unspecified))
        }
    }
}
