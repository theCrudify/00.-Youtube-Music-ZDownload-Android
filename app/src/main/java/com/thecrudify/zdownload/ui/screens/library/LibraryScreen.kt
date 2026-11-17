package com.thecrudify.zdownload.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.library.LibrarySection
import com.thecrudify.zdownload.data.library.TrackSort
import com.thecrudify.zdownload.ui.components.Artwork
import com.thecrudify.zdownload.ui.components.EmptyState
import com.thecrudify.zdownload.ui.components.ScreenHeader
import com.thecrudify.zdownload.ui.components.TrackActionDialogs
import com.thecrudify.zdownload.ui.components.TrackMenu
import com.thecrudify.zdownload.ui.components.TrackRow
import com.thecrudify.zdownload.ui.components.rememberTrackActions
import com.thecrudify.zdownload.ui.theme.AccentCyan
import com.thecrudify.zdownload.ui.theme.BgElevated
import com.thecrudify.zdownload.ui.theme.TextMuted
import com.thecrudify.zdownload.ui.theme.TextPrimary
import com.thecrudify.zdownload.ui.theme.TextSecondary
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.LibraryViewModel

@Composable
private fun sectionLabel(section: LibrarySection) = when (section) {
    LibrarySection.SONGS -> tr("Semua Lagu", "All Songs")
    LibrarySection.ALBUMS -> "Album"
    LibrarySection.ARTISTS -> tr("Artis", "Artists")
    LibrarySection.GENRES -> "Genre"
    LibrarySection.FAVORITES -> tr("Favorit", "Favorites")
    LibrarySection.RECENTLY_ADDED -> tr("Baru Ditambahkan", "Recently Added")
    LibrarySection.RECENTLY_PLAYED -> tr("Terakhir Diputar", "Recently Played")
    LibrarySection.DOWNLOADED -> tr("Unduhan", "Downloaded")
}

@Composable
private fun sortLabel(sort: TrackSort) = when (sort) {
    TrackSort.DATE_ADDED -> tr("Baru ditambahkan", "Recently added")
    TrackSort.LAST_PLAYED -> tr("Terakhir diputar", "Recently played")
    TrackSort.TITLE -> tr("Judul", "Title")
    TrackSort.ARTIST -> tr("Artis", "Artist")
    TrackSort.ALBUM -> "Album"
    TrackSort.DURATION -> tr("Durasi", "Duration")
    TrackSort.FILE_SIZE -> tr("Ukuran file", "File size")
    TrackSort.YEAR -> tr("Tahun", "Year")
}

@Composable
fun LibraryScreen(vm: LibraryViewModel) {
    val section by vm.section.collectAsState()
    val query by vm.query.collectAsState()
    val sort by vm.sort.collectAsState()
    val group by vm.group.collectAsState()
    val tracks by vm.visibleTracks.collectAsState()
    val groups by vm.groups.collectAsState()
    val selection by vm.selection.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val allTracks by vm.allTracks.collectAsState()
    val player by vm.playback.state.collectAsState()
    val actions = rememberTrackActions()
    val selecting = selection.isNotEmpty()
    var sortMenu by remember { mutableStateOf(false) }

    // The system file picker grants access to exactly the files chosen: no storage permission is requested
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.importAudio(it) }

    BackHandler(enabled = selecting || group != null) {
        if (selecting) vm.clearSelection() else vm.group.value = null
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        if (selecting) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::clearSelection) { Icon(Icons.Default.Close, tr("Batal memilih", "Cancel selection"), tint = TextPrimary) }
                Text(tr("${selection.size} dipilih", "${selection.size} selected"), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.selectAll(tracks) }) { Icon(Icons.Default.SelectAll, tr("Pilih semua", "Select all"), tint = TextPrimary) }
                IconButton(onClick = { actions.addingToPlaylist = vm.selectedTracks() }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, tr("Tambah ke playlist", "Add to playlist"), tint = TextPrimary) }
                IconButton(onClick = { actions.exporting = vm.selectedTracks() }) { Icon(Icons.Default.FolderCopy, tr("Ekspor ke folder", "Export to folder"), tint = TextPrimary) }
                IconButton(onClick = { actions.deleting = vm.selectedTracks() }) { Icon(Icons.Default.Delete, tr("Hapus", "Delete"), tint = TextPrimary) }
            }
        } else {
            ScreenHeader(
                title = "Library",
                subtitle = tr("${allTracks.size} lagu tersimpan", "${allTracks.size} songs saved")
            ) {
                IconButton(onClick = { importPicker.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Default.FileOpen, tr("Impor file audio", "Import audio files"), tint = TextPrimary)
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, tr("Urutkan", "Sort"), tint = TextPrimary) }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        TrackSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sortLabel(option)) },
                                trailingIcon = { if (option == sort) Icon(Icons.Default.Check, null, tint = AccentCyan) },
                                onClick = { vm.setSort(option); sortMenu = false }
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            placeholder = { Text(tr("Cari judul, artis, album, genre…", "Search title, artist, album, genre…"), color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Default.Close, tr("Hapus pencarian", "Clear search"), tint = TextMuted) }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibrarySection.entries.forEach { option ->
                FilterChip(
                    selected = option == section,
                    onClick = { vm.setSection(option) },
                    label = { Text(sectionLabel(option)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentCyan.copy(alpha = 0.22f),
                        selectedLabelColor = AccentCyan,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        if (section.isGrouped && group == null) {
            if (groups.isEmpty()) {
                EmptyState(Icons.Default.LibraryMusic, tr("Belum ada apa-apa di sini", "Nothing here yet"), tr("Unduh atau impor lagu, lalu semuanya dikelompokkan otomatis.", "Download or import songs and they are grouped automatically."))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(groups, key = { it.name }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { vm.group.value = item.name }.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Artwork(item.artworkPath, Modifier.size(52.dp), corner = if (section == LibrarySection.ARTISTS) 26.dp else 8.dp)
                            Column(Modifier.weight(1f)) {
                                Text(item.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(tr("${item.trackCount} lagu", "${item.trackCount} songs"), color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            if (group != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.group.value = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Kembali", "Back"), tint = TextPrimary) }
                    Text(group.orEmpty(), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.play(tracks, shuffle = true) }, enabled = tracks.isNotEmpty()) {
                        Icon(Icons.Default.Shuffle, tr("Putar acak", "Shuffle play"), tint = AccentCyan)
                    }
                }
            }
            if (tracks.isEmpty()) {
                EmptyState(
                    Icons.Default.LibraryMusic,
                    if (query.isNotBlank()) tr("Tidak ada hasil untuk \"$query\"", "No results for \"$query\"")
                    else tr("Belum ada lagu di sini", "No songs here yet"),
                    if (allTracks.isEmpty()) tr("Unduh lagu dari tab Beranda, atau impor file audio dengan tombol di kanan atas.", "Download a song from the Home tab, or import audio files with the button at the top right.")
                    else tr("Coba bagian atau kata kunci lain.", "Try another section or search term.")
                )
            } else {
                TrackList(
                    tracks = tracks,
                    currentTrackId = player.current?.trackId,
                    selection = selection,
                    onClick = { index, track -> if (selecting) vm.toggleSelected(track.id) else vm.play(tracks, index, shuffle = false) },
                    onLongClick = { vm.toggleSelected(it.id) }
                ) { track ->
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { vm.toggleFavorite(track) }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            if (track.isFavorite) tr("Hapus dari favorit", "Remove from favorites") else tr("Tambah ke favorit", "Add to favorites"),
                            tint = if (track.isFavorite) androidx.compose.ui.graphics.Color(0xFFF43F5E) else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.MoreVert, tr("Opsi lagu", "Song options"), tint = TextSecondary)
                        }
                        TrackMenu(track, menu, { menu = false }, vm, actions)
                    }
                }
            }
        }
    }

    TrackActionDialogs(actions, vm, playlists)
}

/** Virtualized: only the rows on screen are composed, so the list stays smooth with thousands of songs. */
@Composable
fun TrackList(
    tracks: List<Track>,
    currentTrackId: String?,
    selection: Set<String>,
    onClick: (index: Int, track: Track) -> Unit,
    onLongClick: ((Track) -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable (Track) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                selected = track.id in selection,
                selecting = selection.isNotEmpty(),
                onClick = { onClick(index, track) },
                onLongClick = onLongClick?.let { { it(track) } }
            ) { trailing(track) }
        }
    }
}
