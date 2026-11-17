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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecrudify.zdownload.R
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.prefs.ThemeMode
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
import com.thecrudify.zdownload.viewmodel.LibraryViewModel
import com.thecrudify.zdownload.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    libraryVm: LibraryViewModel,
    mainVm: MainViewModel,
    onBack: () -> Unit
) {
    val stats by libraryVm.storageStats.collectAsState()
    val allTracks by libraryVm.allTracks.collectAsState()
    val defaultFormat by mainVm.selectedFormat.collectAsState()
    val themeMode by libraryVm.themeMode.collectAsState()
    val language by libraryVm.language.collectAsState()
    val defaultSpeed by libraryVm.defaultSpeed.collectAsState()
    val defaultRepeat by libraryVm.defaultRepeat.collectAsState()
    val defaultShuffle by libraryVm.defaultShuffle.collectAsState()

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
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Appearance / Theme Mode
        SectionCard(title = stringResource(R.string.settings_section_appearance), icon = Icons.Default.Palette) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.theme_mode_title),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectablePill(
                        label = stringResource(R.string.theme_system),
                        selected = themeMode == ThemeMode.SYSTEM,
                        modifier = Modifier.weight(1f),
                        onClick = { libraryVm.setThemeMode(ThemeMode.SYSTEM) }
                    )
                    SelectablePill(
                        label = stringResource(R.string.theme_light),
                        selected = themeMode == ThemeMode.LIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = { libraryVm.setThemeMode(ThemeMode.LIGHT) }
                    )
                    SelectablePill(
                        label = stringResource(R.string.theme_dark),
                        selected = themeMode == ThemeMode.DARK,
                        modifier = Modifier.weight(1f),
                        onClick = { libraryVm.setThemeMode(ThemeMode.DARK) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Language Selection
        SectionCard(title = stringResource(R.string.settings_section_language), icon = Icons.Default.Language) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.language_title),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val normalizedLang = language.trim().uppercase()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectablePill(
                        label = stringResource(R.string.language_system),
                        selected = normalizedLang == "SYSTEM" || normalizedLang.isBlank(),
                        modifier = Modifier.weight(1f),
                        onClick = { libraryVm.setLanguage("SYSTEM") }
                    )
                    SelectablePill(
                        label = "Bahasa",
                        selected = normalizedLang == "ID" || normalizedLang == "IN",
                        modifier = Modifier.weight(1f),
                        onClick = { libraryVm.setLanguage("ID") }
                    )
                    SelectablePill(
                        label = "English",
                        selected = normalizedLang == "EN",
                        modifier = Modifier.weight(1f),
                        onClick = { libraryVm.setLanguage("EN") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Playback Defaults
        SectionCard(title = stringResource(R.string.settings_section_playback), icon = Icons.Default.Speed) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Playback speed
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.playback_speed),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        speedOptions.forEach { spd ->
                            SelectablePill(
                                label = "${spd}x",
                                selected = (defaultSpeed - spd).let { kotlin.math.abs(it) < 0.05f },
                                modifier = Modifier.weight(1f),
                                onClick = { libraryVm.setDefaultSpeed(spd) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 2.dp))

                // Repeat mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.playback_repeat),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectablePill(
                            label = "Off",
                            selected = defaultRepeat == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { libraryVm.setDefaultRepeat(0) }
                        )
                        SelectablePill(
                            label = "All",
                            selected = defaultRepeat == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { libraryVm.setDefaultRepeat(1) }
                        )
                        SelectablePill(
                            label = "One",
                            selected = defaultRepeat == 2,
                            modifier = Modifier.weight(1f),
                            onClick = { libraryVm.setDefaultRepeat(2) }
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 2.dp))

                // Shuffle mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.playback_shuffle),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (defaultShuffle) "Aktif / Active" else "Nonaktif / Inactive",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = defaultShuffle,
                        onCheckedChange = { libraryVm.setDefaultShuffle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentCyan,
                            uncheckedTrackColor = BgElevatedHover
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Download Preferences
        SectionCard(title = stringResource(R.string.settings_section_downloads), icon = Icons.Default.MusicNote) {
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
                            text = stringResource(R.string.default_format_title),
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
                            text = stringResource(R.string.change),
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

        // 5. Device Storage
        SectionCard(title = stringResource(R.string.settings_section_storage), icon = Icons.Default.Storage) {
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
                            text = stringResource(R.string.storage_total_music),
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
                            text = stringResource(R.string.storage_free_space),
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
                    label = stringResource(R.string.storage_tracks_in_library),
                    value = "${allTracks.size}"
                )
                StorageStatRow(
                    label = stringResource(R.string.storage_artwork_cache),
                    value = formatBytes(artwork)
                )
                StorageStatRow(
                    label = stringResource(R.string.storage_temp_files),
                    value = formatBytes(temp)
                )
                StorageStatRow(
                    label = stringResource(R.string.storage_total_capacity),
                    value = formatBytes(total)
                )

                stats?.musicPath?.let { path ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.storage_folder_location),
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
                        Text(stringResource(R.string.btn_verify_files), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { libraryVm.clearTemp() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CleaningServices, null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.btn_clear_temp), color = AccentAmber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. File Operations
        SectionCard(title = stringResource(R.string.settings_section_file_ops), icon = Icons.Default.Folder) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionItemRow(
                    icon = Icons.Default.FileOpen,
                    title = stringResource(R.string.import_local_audio),
                    subtitle = stringResource(R.string.import_local_audio_desc),
                    onClick = { importPicker.launch(arrayOf("audio/*")) }
                )

                ActionItemRow(
                    icon = Icons.Default.FolderCopy,
                    title = stringResource(R.string.export_all_music),
                    subtitle = stringResource(R.string.export_all_music_desc),
                    onClick = { exportPicker.launch(null) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. About Application
        SectionCard(title = stringResource(R.string.settings_section_about), icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ZDownload Standalone Mobile Edition",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = stringResource(R.string.app_version_info),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = stringResource(R.string.app_description),
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
private fun SelectablePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AccentCyan.copy(alpha = 0.2f) else BgElevatedHover)
            .border(
                1.dp,
                if (selected) AccentCyan else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AccentCyan else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
