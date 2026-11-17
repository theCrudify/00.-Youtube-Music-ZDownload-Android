package com.thecrudify.zdownload.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.thecrudify.zdownload.data.db.PlaylistWithCount
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.metadata.TagEdit
import com.thecrudify.zdownload.data.metadata.TagWriter
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.LibraryViewModel
import java.io.File

/** Which track dialog is open. Shared by every screen that lists tracks so they behave the same. */
class TrackActionState {
    var editing by mutableStateOf<Track?>(null)
    var deleting by mutableStateOf<List<Track>>(emptyList())
    var addingToPlaylist by mutableStateOf<List<Track>>(emptyList())
    var exporting by mutableStateOf<List<Track>>(emptyList())
}

@Composable
fun rememberTrackActions(): TrackActionState = remember { TrackActionState() }

/** Hands the audio files to the system share sheet (send to another app, Bluetooth, Drive…). */
fun shareTracks(context: Context, tracks: List<Track>) {
    val uris = tracks.map { File(it.entity.filePath) }.filter { it.isFile }.mapNotNull {
        runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }.getOrNull()
    }
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
    }
    intent.setType("audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(intent, null)) }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        onClick = onClick
    )
}

/** The "⋮" menu of a track. [extra] adds screen-specific entries (e.g. "remove from this playlist"). */
@Composable
fun TrackMenu(
    track: Track,
    expanded: Boolean,
    onDismiss: () -> Unit,
    vm: LibraryViewModel,
    actions: TrackActionState,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuItem(Icons.Default.SkipNext, tr("Putar berikutnya", "Play next")) { vm.playNext(listOf(track)); onDismiss() }
        MenuItem(Icons.AutoMirrored.Filled.QueueMusic, tr("Tambah ke antrean", "Add to queue")) { vm.addToQueue(listOf(track)); onDismiss() }
        MenuItem(Icons.AutoMirrored.Filled.PlaylistAdd, tr("Tambah ke playlist", "Add to playlist")) { actions.addingToPlaylist = listOf(track); onDismiss() }
        MenuItem(Icons.Default.Edit, tr("Edit info lagu", "Edit song info")) { actions.editing = track; onDismiss() }
        MenuItem(Icons.Default.Share, tr("Bagikan / ekspor file", "Share / export file")) { shareTracks(context, listOf(track)); onDismiss() }
        extra()
        MenuItem(Icons.Default.Delete, tr("Hapus", "Delete")) { actions.deleting = listOf(track); onDismiss() }
    }
}

/** Renders whichever dialog [state] has open. Place once per screen. */
@Composable
fun TrackActionDialogs(state: TrackActionState, vm: LibraryViewModel, playlists: List<PlaylistWithCount>) {
    state.editing?.let { track ->
        EditTrackDialog(
            track = track,
            onSave = { edit, artwork, writeToFile ->
                vm.saveMetadata(track, edit, artwork, writeToFile)
                state.editing = null
            },
            onDismiss = { state.editing = null }
        )
    }
    if (state.deleting.isNotEmpty()) {
        DeleteTracksDialog(
            count = state.deleting.size,
            onConfirm = { deleteFiles ->
                vm.delete(state.deleting, deleteFiles)
                state.deleting = emptyList()
            },
            onDismiss = { state.deleting = emptyList() }
        )
    }
    if (state.addingToPlaylist.isNotEmpty()) {
        val ids = state.addingToPlaylist.map { it.id }
        PlaylistPickerDialog(
            playlists = playlists,
            onPick = { vm.addToPlaylist(it, ids); state.addingToPlaylist = emptyList() },
            onCreate = { name -> vm.createPlaylist(name, ids); state.addingToPlaylist = emptyList() },
            onDismiss = { state.addingToPlaylist = emptyList() }
        )
    }
    // Export: the system folder picker grants access to one folder the user chooses, no storage permission needed
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.export(state.exporting, uri)
        state.exporting = emptyList()
    }
    LaunchedEffect(state.exporting) {
        if (state.exporting.isNotEmpty()) folderPicker.launch(null)
    }
}

@Composable
fun TextInputDialog(title: String, initial: String, confirmLabel: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it.take(80) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) } }
    )
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) } }
    )
}

@Composable
private fun DeleteTracksDialog(count: Int, onConfirm: (deleteFiles: Boolean) -> Unit, onDismiss: () -> Unit) {
    // Off by default: removing from the library must never silently destroy the user's file
    var deleteFiles by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Hapus $count lagu dari library?", "Remove $count song(s) from library?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Lagu juga akan keluar dari semua playlist dan favorit.", "They will also be removed from all playlists and favorites."))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { deleteFiles = !deleteFiles }
                ) {
                    Checkbox(checked = deleteFiles, onCheckedChange = { deleteFiles = it })
                    Text(tr("Hapus juga file audionya dari perangkat", "Also delete the audio file from this device"))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(deleteFiles) }) {
                Text(if (deleteFiles) tr("Hapus permanen", "Delete permanently") else tr("Hapus dari library", "Remove from library"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) } }
    )
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistWithCount>,
    onPick: (PlaylistWithCount) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    if (creating) {
        TextInputDialog(tr("Playlist baru", "New playlist"), "", tr("Buat", "Create"), onConfirm = onCreate, onDismiss = onDismiss)
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Tambah ke playlist", "Add to playlist")) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AccentCyan)
                        Text(tr("Playlist baru…", "New playlist…"), color = AccentCyan)
                    }
                }
                items(playlists, key = { it.playlist.id }) { playlist ->
                    Column(modifier = Modifier.fillMaxWidth().clickable { onPick(playlist) }.padding(vertical = 10.dp)) {
                        Text(playlist.playlist.name, color = TextPrimary)
                        Text(tr("${playlist.trackCount} lagu", "${playlist.trackCount} songs"), color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) } }
    )
}

@Composable
private fun EditTrackDialog(
    track: Track,
    onSave: (TagEdit, artwork: Uri?, writeToFile: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(track.title) }
    var artist by rememberSaveable { mutableStateOf(track.artistName) }
    var album by rememberSaveable { mutableStateOf(track.album) }
    var albumArtist by rememberSaveable { mutableStateOf(track.albumArtist.orEmpty()) }
    var genre by rememberSaveable { mutableStateOf(track.entity.genre) }
    var year by rememberSaveable { mutableStateOf(track.entity.year?.toString().orEmpty()) }
    var trackNumber by rememberSaveable { mutableStateOf(track.entity.trackNumber?.toString().orEmpty()) }
    var lyrics by rememberSaveable { mutableStateOf(track.entity.lyrics.orEmpty()) }
    var artwork by remember { mutableStateOf<Uri?>(null) }
    val canWrite = TagWriter.canWrite(track.entity.format)
    var writeToFile by rememberSaveable { mutableStateOf(canWrite) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) artwork = it }

    @Composable
    fun Field(label: String, value: String, numeric: Boolean = false, onChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { v -> onChange(if (numeric) v.filter(Char::isDigit).take(4) else v.take(200)) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Edit info lagu", "Edit song info")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (artwork != null) {
                        coil.compose.AsyncImage(model = artwork, contentDescription = null, modifier = Modifier.size(64.dp))
                    } else {
                        Artwork(track.entity.artworkPath, Modifier.size(64.dp))
                    }
                    TextButton(onClick = { imagePicker.launch("image/*") }) { Text(tr("Ganti sampul", "Change cover")) }
                }
                Field(tr("Judul", "Title"), title) { title = it }
                Field(tr("Artis", "Artist"), artist) { artist = it }
                Field("Album", album) { album = it }
                Field(tr("Artis album", "Album artist"), albumArtist) { albumArtist = it }
                Field("Genre", genre) { genre = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Field(tr("Tahun", "Year"), year, numeric = true) { year = it } }
                    Column(Modifier.weight(1f)) { Field(tr("No. lagu", "Track no."), trackNumber, numeric = true) { trackNumber = it } }
                }
                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it.take(5000) },
                    label = { Text(tr("Lirik Lagu", "Lyrics")) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = canWrite) { writeToFile = !writeToFile }
                ) {
                    Checkbox(checked = writeToFile, enabled = canWrite, onCheckedChange = { writeToFile = it })
                    Text(tr("Tulis juga ke tag di dalam file", "Also write to the file's own tags"), color = if (canWrite) TextPrimary else TextMuted)
                }
                Text(
                    if (canWrite) tr(
                        "Tanpa centang, perubahan hanya disimpan di library aplikasi ini. Dengan centang, pemutar lain juga melihatnya.",
                        "Unchecked, the change is kept in this app's library only. Checked, other players see it too."
                    ) else tr(
                        "Format .${track.entity.format} tidak mendukung penulisan tag, jadi perubahan disimpan di library aplikasi ini saja. File audio tidak diubah.",
                        ".${track.entity.format} files can't have tags written, so the change is kept in this app's library only. The audio file is not modified."
                    ),
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        TagEdit(
                            title = title,
                            artist = artist,
                            album = album,
                            albumArtist = albumArtist,
                            genre = genre,
                            year = year.toIntOrNull(),
                            trackNumber = trackNumber.toIntOrNull(),
                            lyrics = lyrics.trim().takeIf { it.isNotEmpty() }
                        ),
                        artwork, writeToFile && canWrite
                    )
                }
            ) { Text(tr("Simpan", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) } }
    )
}
