package com.thecrudify.zdownload.ui.screens.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thecrudify.zdownload.ui.components.ConfirmDialog
import com.thecrudify.zdownload.ui.components.EmptyState
import com.thecrudify.zdownload.ui.components.ScreenHeader
import com.thecrudify.zdownload.ui.components.TextInputDialog
import com.thecrudify.zdownload.ui.components.TrackActionDialogs
import com.thecrudify.zdownload.ui.components.TrackMenu
import com.thecrudify.zdownload.ui.components.TrackRow
import com.thecrudify.zdownload.ui.components.formatDuration
import com.thecrudify.zdownload.ui.components.rememberTrackActions
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.LibraryViewModel

@Composable
fun PlaylistsScreen(vm: LibraryViewModel) {
    val playlists by vm.playlists.collectAsState()
    val openId by vm.openPlaylistId.collectAsState()
    var creating by remember { mutableStateOf(false) }

    val open = playlists.firstOrNull { it.playlist.id == openId }
    if (open != null) {
        PlaylistDetail(vm, open.playlist.id, open.playlist.name)
        return
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Playlist", tr("${playlists.size} playlist", "${playlists.size} playlists")) {
            IconButton(onClick = { creating = true }) { Icon(Icons.Default.Add, tr("Playlist baru", "New playlist")) }
        }
        if (playlists.isEmpty()) {
            EmptyState(
                Icons.AutoMirrored.Filled.QueueMusic,
                tr("Belum ada playlist", "No playlists yet"),
                tr("Buat playlist dengan tombol + di atas, lalu tambahkan lagu dari Library.", "Create one with the + button above, then add songs from the Library.")
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.playlist.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { vm.openPlaylistId.value = item.playlist.id }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(tr("${item.trackCount} lagu", "${item.trackCount} songs"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        TextInputDialog(tr("Playlist baru", "New playlist"), "", tr("Buat", "Create"), onConfirm = { vm.createPlaylist(it); creating = false }, onDismiss = { creating = false })
    }
}

@Composable
private fun PlaylistDetail(vm: LibraryViewModel, playlistId: Long, name: String) {
    val tracks by vm.playlistTracks.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val player by vm.playback.state.collectAsState()
    val actions = rememberTrackActions()
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    BackHandler { vm.openPlaylistId.value = null }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(
            title = name,
            subtitle = tr("${tracks.size} lagu", "${tracks.size} songs") + " · " + formatDuration(tracks.sumOf { it.entity.durationMs }),
            onBack = { vm.openPlaylistId.value = null }
        ) {
            IconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, tr("Ganti nama", "Rename")) }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Default.Delete, tr("Hapus playlist", "Delete playlist")) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.play(tracks, shuffle = false) }, enabled = tracks.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(tr("Putar", "Play"))
            }
            FilledTonalButton(onClick = { vm.play(tracks, shuffle = true) }, enabled = tracks.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(tr("Acak", "Shuffle"))
            }
        }
        if (tracks.isEmpty()) {
            EmptyState(
                Icons.AutoMirrored.Filled.QueueMusic,
                tr("Playlist ini masih kosong", "This playlist is empty"),
                tr("Buka Library, ketuk ⋮ pada lagu, lalu pilih \"Tambah ke playlist\".", "Open the Library, tap ⋮ on a song, then choose \"Add to playlist\".")
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == player.current?.trackId,
                        onClick = { vm.play(tracks, index, shuffle = false) }
                    ) {
                        var menu by remember { mutableStateOf(false) }
                        // shortcut: up/down buttons instead of drag-and-drop; swap in a reorder library if drag is wanted
                        IconButton(onClick = { vm.movePlaylistTrack(playlistId, index, index - 1) }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, tr("Naikkan", "Move up"))
                        }
                        IconButton(onClick = { vm.movePlaylistTrack(playlistId, index, index + 1) }, enabled = index < tracks.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, tr("Turunkan", "Move down"))
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, tr("Opsi lagu", "Song options")) }
                            TrackMenu(track, menu, { menu = false }, vm, actions) {
                                DropdownMenuItem(
                                    text = { Text(tr("Keluarkan dari playlist ini", "Remove from this playlist")) },
                                    leadingIcon = { Icon(Icons.Default.PlaylistRemove, null, modifier = Modifier.size(20.dp)) },
                                    onClick = { vm.removeFromPlaylist(playlistId, track.id); menu = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    TrackActionDialogs(actions, vm, playlists)
    if (renaming) {
        TextInputDialog(tr("Ganti nama playlist", "Rename playlist"), name, tr("Simpan", "Save"), onConfirm = { vm.renamePlaylist(playlistId, it); renaming = false }, onDismiss = { renaming = false })
    }
    if (deleting) {
        ConfirmDialog(
            tr("Hapus playlist \"$name\"?", "Delete playlist \"$name\"?"),
            tr("Lagu-lagunya tetap ada di Library.", "Its songs stay in the Library."),
            tr("Hapus", "Delete"),
            onConfirm = { vm.deletePlaylist(playlistId); deleting = false },
            onDismiss = { deleting = false }
        )
    }
}
