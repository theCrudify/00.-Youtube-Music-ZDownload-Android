package com.thecrudify.zdownload.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thecrudify.zdownload.R
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.prefs.ThemeMode
import com.thecrudify.zdownload.ui.components.ScreenHeader
import com.thecrudify.zdownload.ui.components.formatBytes
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.LibraryViewModel
import com.thecrudify.zdownload.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(bottom = 24.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
            onBack = onBack
        )

        // 1. Appearance
        SectionTitle(stringResource(R.string.settings_section_appearance), first = true)
        LabeledSetting(stringResource(R.string.theme_mode_title)) {
            Segments(
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark)
                ),
                isSelected = { it == themeMode },
                onSelect = libraryVm::setThemeMode
            )
        }

        // 2. Language
        SectionTitle(stringResource(R.string.settings_section_language))
        LabeledSetting(stringResource(R.string.language_title)) {
            val normalizedLang = language.trim().uppercase()
            Segments(
                options = listOf("SYSTEM" to stringResource(R.string.language_system), "ID" to "Indonesia", "EN" to "English"),
                isSelected = {
                    when (it) {
                        "SYSTEM" -> normalizedLang == "SYSTEM" || normalizedLang.isBlank()
                        "ID" -> normalizedLang == "ID" || normalizedLang == "IN"
                        else -> normalizedLang == "EN"
                    }
                },
                onSelect = { libraryVm.setLanguage(it) }
            )
        }

        // 3. Playback defaults
        SectionTitle(stringResource(R.string.settings_section_playback))
        LabeledSetting(stringResource(R.string.playback_speed)) {
            Segments(
                options = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).map { it to "${it}x" },
                isSelected = { kotlin.math.abs(defaultSpeed - it) < 0.05f },
                onSelect = { libraryVm.setDefaultSpeed(it) },
                // Five segments are too narrow for the check mark on small phones.
                showCheck = false
            )
        }
        LabeledSetting(stringResource(R.string.playback_repeat)) {
            Segments(
                options = listOf(0 to "Off", 1 to "All", 2 to "One"),
                isSelected = { it == defaultRepeat },
                onSelect = { libraryVm.setDefaultRepeat(it) }
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.playback_shuffle)) },
            supportingContent = { Text(if (defaultShuffle) tr("Aktif", "On") else tr("Nonaktif", "Off")) },
            trailingContent = { Switch(checked = defaultShuffle, onCheckedChange = { libraryVm.setDefaultShuffle(it) }) },
            modifier = Modifier.clickable { libraryVm.setDefaultShuffle(!defaultShuffle) }
        )

        // 4. Downloads
        SectionTitle(stringResource(R.string.settings_section_downloads))
        ListItem(
            headlineContent = { Text(stringResource(R.string.default_format_title)) },
            supportingContent = { Text("${defaultFormat.title} (${defaultFormat.badge})") },
            trailingContent = {
                Box {
                    Text(stringResource(R.string.change), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    DropdownMenu(expanded = formatDropdownExpanded, onDismissRequest = { formatDropdownExpanded = false }) {
                        AudioFormat.entries.forEach { format ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(format.title)
                                        Text(format.badge, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            },
            modifier = Modifier.clickable { formatDropdownExpanded = true }
        )

        // 5. Storage
        SectionTitle(stringResource(R.string.settings_section_storage))
        val total = stats?.totalBytes ?: 1L
        val free = stats?.freeBytes ?: 0L
        val music = stats?.musicBytes ?: 0L
        val artwork = stats?.artworkBytes ?: 0L
        val temp = stats?.tempBytes ?: 0L
        val usedFraction = ((music + artwork + temp).toFloat() / total.toFloat()).coerceIn(0f, 1f)

        StatRow(stringResource(R.string.storage_total_music), formatBytes(music))
        StatRow(stringResource(R.string.storage_free_space), formatBytes(free))
        LinearProgressIndicator(
            progress = { usedFraction },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        StatRow(stringResource(R.string.storage_tracks_in_library), "${allTracks.size}")
        StatRow(stringResource(R.string.storage_artwork_cache), formatBytes(artwork))
        StatRow(stringResource(R.string.storage_temp_files), formatBytes(temp))
        StatRow(stringResource(R.string.storage_total_capacity), formatBytes(total))
        stats?.musicPath?.let { path ->
            ListItem(
                headlineContent = { Text(stringResource(R.string.storage_folder_location)) },
                supportingContent = { Text(path) }
            )
        }
        ActionRow(Icons.Default.Verified, stringResource(R.string.btn_verify_files)) { libraryVm.verifyLibrary() }
        ActionRow(Icons.Default.CleaningServices, stringResource(R.string.btn_clear_temp)) { libraryVm.clearTemp() }

        // 6. File operations
        SectionTitle(stringResource(R.string.settings_section_file_ops))
        ActionRow(Icons.Default.FileOpen, stringResource(R.string.import_local_audio), stringResource(R.string.import_local_audio_desc)) {
            importPicker.launch(arrayOf("audio/*"))
        }
        ActionRow(Icons.Default.FolderCopy, stringResource(R.string.export_all_music), stringResource(R.string.export_all_music_desc)) {
            exportPicker.launch(null)
        }

        // 7. About
        SectionTitle(stringResource(R.string.settings_section_about))
        ListItem(
            headlineContent = { Text(stringResource(R.string.app_name)) },
            supportingContent = { Text(stringResource(R.string.app_version_info)) }
        )
        ListItem(
            headlineContent = { Text(tr("Dibuat oleh", "Made by")) },
            supportingContent = { Text(stringResource(R.string.author)) }
        )
        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String, first: Boolean = false) {
    if (!first) HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun LabeledSetting(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segments(
    options: List<Pair<T, String>>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    showCheck: Boolean = true
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            val selected = isSelected(value)
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = { if (showCheck) SegmentedButtonDefaults.Icon(selected) },
                label = { Text(label, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium) }
    )
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
