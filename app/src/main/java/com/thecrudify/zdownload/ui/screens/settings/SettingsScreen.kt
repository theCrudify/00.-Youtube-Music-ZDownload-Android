package com.thecrudify.zdownload.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.ui.components.ScreenHeader
import com.thecrudify.zdownload.ui.components.formatBytes
import com.thecrudify.zdownload.ui.theme.AccentAmber
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.AccentEmerald
import com.thecrudify.zdownload.ui.theme.BgBase
import com.thecrudify.zdownload.ui.theme.BgElevated
import com.thecrudify.zdownload.ui.theme.BgElevatedHover
import com.thecrudify.zdownload.ui.theme.BorderSubtle
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.LibraryViewModel
import com.thecrudify.zdownload.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    libraryVm: LibraryViewModel,
    mainVm: MainViewModel
) {
    val stats by libraryVm.storageStats.collectAsState()
    val allTracks by libraryVm.allTracks.collectAsState()
    val defaultFormat by mainVm.selectedFormat.collectAsState()
    val language by libraryVm.language.collectAsState()

    var formatDropdownExpanded by remember { mutableStateOf(false) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        libraryVm.importAudio(it)
    }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) libraryVm.export(allTracks, uri)
    }

    LaunchedEffect(Unit) {
        libraryVm.refreshStorage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        ScreenHeader(
            title = tr("Pengaturan & Penyimpanan", "Settings & Storage"),
            subtitle = tr("Konfigurasi aplikasi dan manajemen file musik", "App configuration and storage management")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Storage Stats Card
        SectionCard(title = tr("Penyimpanan Perangkat", "Device Storage"), icon = Icons.Default.Storage) {
            val total = stats?.totalBytes ?: 1L
            val free = stats?.freeBytes ?: 0L
            val music = stats?.musicBytes ?: 0L
            val artwork = stats?.artworkBytes ?: 0L
            val temp = stats?.tempBytes ?: 0L
            val usedByApp = music + artwork + temp
            val usedFraction = (usedByApp.toFloat() / total.toFloat()).coerceIn(0f, 1f)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("Total Musik", "Total Music"),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatBytes(music),
                            color = AccentCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = tr("Ruang Tersisa", "Free Space"),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatBytes(free),
                            color = AccentEmerald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { usedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentCyan,
                    trackColor = BorderSubtle
                )

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                StorageStatRow(
                    label = tr("Jumlah lagu di library", "Tracks in library"),
                    value = "${allTracks.size} lagu"
                )
                StorageStatRow(
                    label = tr("Sampul album (cache)", "Cover art (cache)"),
                    value = formatBytes(artwork)
                )
                StorageStatRow(
                    label = tr("File sementara / unduhan parsial", "Temporary / partial files"),
                    value = formatBytes(temp)
                )
                StorageStatRow(
                    label = tr("Kapasitas penyimpanan", "Total storage capacity"),
                    value = formatBytes(total)
                )

                stats?.musicPath?.let { path ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr("Lokasi Folder Musik:", "Music Directory:"),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = path,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { libraryVm.verifyLibrary() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgBase),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Verified, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(tr("Periksa File", "Verify Files"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { libraryVm.clearTemp() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CleaningServices, null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(tr("Bersihkan Temp", "Clear Temp"), color = AccentAmber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. File Import / Export Operations
        SectionCard(title = tr("Operasi File & Ekspor", "File Operations & Export"), icon = Icons.Default.Folder) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionItemRow(
                    icon = Icons.Default.FileOpen,
                    title = tr("Impor File Audio Lokal", "Import Local Audio Files"),
                    subtitle = tr("Tambahkan MP3, FLAC, M4A, WAV, OPUS dari penyimpanan", "Add audio files from your device storage"),
                    onClick = { importPicker.launch(arrayOf("audio/*")) }
                )

                ActionItemRow(
                    icon = Icons.Default.FolderCopy,
                    title = tr("Ekspor Seluruh Musik", "Export All Music"),
                    subtitle = tr("Salin semua lagu ke folder eksternal atau kartu SD", "Copy all library songs to another folder or SD card"),
                    onClick = { exportPicker.launch(null) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Audio & Format Preferences
        SectionCard(title = tr("Preferensi Audio & Unduhan", "Audio & Download Preferences"), icon = Icons.Default.MusicNote) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { formatDropdownExpanded = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Format Unduhan Default", "Default Download Format"),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${defaultFormat.title} (${defaultFormat.badge})",
                            color = AccentCyan,
                            fontSize = 13.sp
                        )
                    }

                    Box {
                        Text(
                            text = tr("Ubah", "Change"),
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        DropdownMenu(
                            expanded = formatDropdownExpanded,
                            onDismissRequest = { formatDropdownExpanded = false }
                        ) {
                            AudioFormat.entries.forEach { format ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(format.title, fontWeight = FontWeight.Bold)
                                            Text(format.badge, color = TextMuted, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        mainVm.selectFormat(format)
                                        formatDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Language & System
        SectionCard(title = tr("Bahasa & Tampilan", "Language & Appearance"), icon = Icons.Default.Language) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Bahasa Aplikasi", "App Language"),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguagePill(
                        label = "Bahasa Indonesia",
                        selected = language == "ID",
                        onClick = { libraryVm.setLanguage("ID") }
                    )
                    LanguagePill(
                        label = "English",
                        selected = language == "EN",
                        onClick = { libraryVm.setLanguage("EN") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. App Info
        SectionCard(title = tr("Tentang ZDownload", "About ZDownload"), icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ZDownload Standalone Mobile Edition",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = tr("Versi 2.0.0 (Media3 ExoPlayer • yt-dlp on-device • Room DB)", "Version 2.0.0 (Media3 ExoPlayer • on-device yt-dlp • Room DB)"),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = tr(
                        "Aplikasi pemutar musik offline & pengunduh YouTube berstandar profesional. Sepenuhnya mandiri tanpa server perantara.",
                        "Professional offline music player & YouTube downloader. Fully autonomous on-device engine without intermediate servers."
                    ),
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StorageStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ActionItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BgElevatedHover),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LanguagePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AccentCyan.copy(alpha = 0.2f) else BgElevatedHover)
            .border(
                1.dp,
                if (selected) AccentCyan else BorderSubtle,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) AccentCyan else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
