package com.thecrudify.zdownload.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thecrudify.zdownload.R
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.library.LibrarySection
import com.thecrudify.zdownload.data.library.TrackGroup
import com.thecrudify.zdownload.data.library.TrackSort
import com.thecrudify.zdownload.ui.components.Artwork
import com.thecrudify.zdownload.ui.components.EmptyState
import com.thecrudify.zdownload.ui.components.TrackActionDialogs
import com.thecrudify.zdownload.ui.components.TrackActionState
import com.thecrudify.zdownload.ui.components.TrackMenu
import com.thecrudify.zdownload.ui.components.TrackRow
import com.thecrudify.zdownload.ui.components.rememberTrackActions
import com.thecrudify.zdownload.ui.tr
import com.thecrudify.zdownload.viewmodel.LibraryViewModel

/** Chip order follows the usual music-library convention: songs, personal filters, then groupings. */
private val chipOrder = listOf(
    LibrarySection.SONGS, LibrarySection.FAVORITES, LibrarySection.ALBUMS, LibrarySection.ARTISTS,
    LibrarySection.GENRES, LibrarySection.RECENTLY_ADDED, LibrarySection.RECENTLY_PLAYED, LibrarySection.DOWNLOADED
)

@Composable
private fun sectionLabel(section: LibrarySection) = when (section) {
    LibrarySection.SONGS -> tr("Lagu", "Songs")
    LibrarySection.ALBUMS -> "Album"
    LibrarySection.ARTISTS -> tr("Artis", "Artists")
    LibrarySection.GENRES -> "Genre"
    LibrarySection.FAVORITES -> tr("Favorit", "Favorites")
    LibrarySection.RECENTLY_ADDED -> tr("Baru ditambah", "Recently added")
    LibrarySection.RECENTLY_PLAYED -> tr("Baru diputar", "Recently played")
    LibrarySection.DOWNLOADED -> tr("Diunduh", "Downloaded")
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
private fun groupSubtitle(section: LibrarySection, group: TrackGroup): String {
    val count = tr("${group.trackCount} lagu", "${group.trackCount} songs")
    return when (section) {
        LibrarySection.ALBUMS -> "Album · $count"
        LibrarySection.ARTISTS -> tr("Artis", "Artist") + " · $count"
        else -> count
    }
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
    var searching by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    var grid by rememberSaveable { mutableStateOf(false) }

    // The system file picker grants access to exactly the files chosen: no storage permission is requested
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.importAudio(it) }

    BackHandler(enabled = selecting || group != null) {
        if (selecting) vm.clearSelection() else vm.group.value = null
    }

    val isArtists = section == LibrarySection.ARTISTS
    val currentId = player.current?.trackId
    val trackClick: (Int, Track) -> Unit = { index, track ->
        if (selecting) vm.toggleSelected(track.id) else vm.play(tracks, index, shuffle = false)
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        when {
            selecting -> SelectionBar(selection.size, vm, actions, tracks)
            group != null -> Row(Modifier.padding(start = 4.dp, top = 4.dp)) {
                IconButton(onClick = { vm.group.value = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Kembali", "Back")) }
            }
            else -> Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.nav_library), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
                IconButton(onClick = {
                    if (searching) vm.query.value = ""
                    searching = !searching
                }) {
                    Icon(
                        if (searching) Icons.Default.Close else Icons.Default.Search,
                        if (searching) tr("Tutup pencarian", "Close search") else tr("Cari", "Search")
                    )
                }
                IconButton(onClick = { importPicker.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Default.FileOpen, tr("Impor file audio", "Import audio files"))
                }
            }
        }

        if (group == null) {
            if (searching) SearchField(query) { vm.query.value = it }
            SectionChips(section) { vm.setSection(it) }
            SortRow(sort, grid, onSort = { vm.setSort(it) }, onToggleGrid = { grid = !grid })
        }

        when {
            group != null -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    GroupHeader(
                        name = group.orEmpty(),
                        artworkPath = groups.firstOrNull { it.name == group }?.artworkPath ?: tracks.firstOrNull()?.entity?.artworkPath,
                        trackCount = tracks.size,
                        shape = if (isArtists) CircleShape else MaterialTheme.shapes.medium,
                        enabled = tracks.isNotEmpty(),
                        onPlay = { vm.play(tracks) },
                        onShuffle = { vm.play(tracks, shuffle = true) }
                    )
                }
                trackRows(tracks, currentId, selection, vm, actions, trackClick)
            }

            section.isGrouped && groups.isEmpty() -> EmptyState(
                Icons.Default.LibraryMusic,
                tr("Belum ada apa-apa di sini", "Nothing here yet"),
                tr("Unduh atau impor lagu, lalu semuanya dikelompokkan otomatis.", "Download or import songs and they are grouped automatically.")
            )

            section.isGrouped && grid -> LibraryGrid {
                gridItemsIndexed(groups, key = { _, g -> g.name }) { _, g ->
                    GridCell(
                        artworkPath = g.artworkPath,
                        title = g.name,
                        subtitle = groupSubtitle(section, g),
                        shape = if (isArtists) CircleShape else MaterialTheme.shapes.medium,
                        centered = isArtists,
                        onClick = { vm.group.value = g.name }
                    )
                }
            }

            section.isGrouped -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(groups, key = { _, g -> g.name }) { _, g ->
                    ListRow(title = g.name, subtitle = groupSubtitle(section, g), onClick = { vm.group.value = g.name }) {
                        Artwork(g.artworkPath, Modifier.size(56.dp), shape = if (isArtists) CircleShape else MaterialTheme.shapes.small)
                    }
                }
            }

            tracks.isEmpty() -> EmptyState(
                Icons.Default.LibraryMusic,
                if (query.isNotBlank()) tr("Tidak ada hasil untuk \"$query\"", "No results for \"$query\"")
                else tr("Belum ada lagu di sini", "No songs here yet"),
                if (allTracks.isEmpty()) tr("Unduh lagu dari tab Unduhan, atau impor file audio dengan tombol di kanan atas.", "Download a song from the Downloads tab, or import audio files with the button at the top right.")
                else tr("Coba bagian atau kata kunci lain.", "Try another section or search term.")
            )

            else -> {
                val showLiked = section == LibrarySection.SONGS && query.isBlank()
                val likedCount = allTracks.count { it.isFavorite }
                val openLiked = { vm.setSection(LibrarySection.FAVORITES) }
                if (grid) {
                    LibraryGrid {
                        if (showLiked) item(span = { GridItemSpan(maxLineSpan) }) { LikedSongsRow(likedCount, openLiked, inset = 0.dp) }
                        gridItemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                            TrackCell(
                                track = track,
                                isCurrent = track.id == currentId,
                                selected = track.id in selection,
                                selecting = selecting,
                                onClick = { trackClick(index, track) },
                                onLongClick = { vm.toggleSelected(track.id) },
                                vm = vm,
                                actions = actions
                            )
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (showLiked) item { LikedSongsRow(likedCount, openLiked) }
                        trackRows(tracks, currentId, selection, vm, actions, trackClick)
                    }
                }
            }
        }
    }

    TrackActionDialogs(actions, vm, playlists)
}

@Composable
private fun SelectionBar(count: Int, vm: LibraryViewModel, actions: TrackActionState, tracks: List<Track>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = vm::clearSelection) { Icon(Icons.Default.Close, tr("Batal memilih", "Cancel selection")) }
        Text(tr("$count dipilih", "$count selected"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { vm.selectAll(tracks) }) { Icon(Icons.Default.SelectAll, tr("Pilih semua", "Select all")) }
        IconButton(onClick = { actions.addingToPlaylist = vm.selectedTracks() }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, tr("Tambah ke playlist", "Add to playlist")) }
        IconButton(onClick = { actions.exporting = vm.selectedTracks() }) { Icon(Icons.Default.FolderCopy, tr("Ekspor ke folder", "Export to folder")) }
        IconButton(onClick = { actions.deleting = vm.selectedTracks() }) { Icon(Icons.Default.Delete, tr("Hapus", "Delete")) }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text(tr("Cari judul, artis, album, genre…", "Search title, artist, album, genre…")) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, tr("Hapus pencarian", "Clear search")) }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).focusRequester(focus)
    )
}

@Composable
private fun SectionChips(section: LibrarySection, onSelect: (LibrarySection) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (section != LibrarySection.SONGS) {
            FilterChip(
                selected = false,
                onClick = { onSelect(LibrarySection.SONGS) },
                label = { Icon(Icons.Default.Close, tr("Hapus filter", "Clear filter"), Modifier.size(18.dp)) }
            )
        }
        chipOrder.forEach { option ->
            FilterChip(selected = option == section, onClick = { onSelect(option) }, label = { Text(sectionLabel(option)) })
        }
    }
}

@Composable
private fun SortRow(sort: TrackSort, grid: Boolean, onSort: (TrackSort) -> Unit, onToggleGrid: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(onClick = { menu = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(sortLabel(sort))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                TrackSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(sortLabel(option)) },
                        trailingIcon = { if (option == sort) Icon(Icons.Default.Check, null) },
                        onClick = { onSort(option); menu = false }
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleGrid) {
            Icon(
                if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                if (grid) tr("Tampilan daftar", "List view") else tr("Tampilan grid", "Grid view")
            )
        }
    }
}

@Composable
private fun LibraryGrid(content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun ListRow(title: String, subtitle: String, onClick: () -> Unit, inset: Dp = 16.dp, art: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = inset, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        art()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LikedSongsRow(count: Int, onClick: () -> Unit, inset: Dp = 16.dp) {
    ListRow(title = tr("Lagu yang disukai", "Liked songs"), subtitle = "Playlist · " + tr("$count lagu", "$count songs"), onClick = onClick, inset = inset) {
        Box(
            Modifier.size(56.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun GridCell(artworkPath: String?, title: String, subtitle: String, shape: Shape, centered: Boolean, onClick: () -> Unit) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick)) {
        Artwork(artworkPath, Modifier.fillMaxWidth().aspectRatio(1f), shape = shape)
        Text(
            title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = align, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Text(
            subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackCell(
    track: Track,
    isCurrent: Boolean,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    vm: LibraryViewModel,
    actions: TrackActionState
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box {
            Artwork(track.entity.artworkPath, Modifier.fillMaxWidth().aspectRatio(1f), shape = MaterialTheme.shapes.medium)
            if (selecting) {
                Icon(
                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) tr("Dipilih", "Selected") else tr("Belum dipilih", "Not selected"),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                Text(
                    track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    track.artistName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, tr("Opsi lagu", "Song options"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TrackMenu(track, menu, { menu = false }, vm, actions)
            }
        }
    }
}

@Composable
private fun GroupHeader(
    name: String,
    artworkPath: String?,
    trackCount: Int,
    shape: Shape,
    enabled: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Artwork(artworkPath, Modifier.size(200.dp), shape = shape)
        Text(
            name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() }
        )
        Text(tr("$trackCount lagu", "$trackCount songs"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = onShuffle, enabled = enabled) { Icon(Icons.Default.Shuffle, tr("Putar acak", "Shuffle play")) }
            FilledIconButton(onClick = onPlay, enabled = enabled, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.PlayArrow, tr("Putar", "Play"), Modifier.size(32.dp))
            }
        }
    }
}

/** Song rows with favorite + overflow actions; LazyColumn composes only the rows on screen. */
private fun LazyListScope.trackRows(
    tracks: List<Track>,
    currentTrackId: String?,
    selection: Set<String>,
    vm: LibraryViewModel,
    actions: TrackActionState,
    onClick: (Int, Track) -> Unit
) {
    itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
        TrackRow(
            track = track,
            isCurrent = track.id == currentTrackId,
            selected = track.id in selection,
            selecting = selection.isNotEmpty(),
            onClick = { onClick(index, track) },
            onLongClick = { vm.toggleSelected(track.id) }
        ) {
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { vm.toggleFavorite(track) }) {
                Icon(
                    if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (track.isFavorite) tr("Hapus dari favorit", "Remove from favorites") else tr("Tambah ke favorit", "Add to favorites"),
                    tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, tr("Opsi lagu", "Song options"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TrackMenu(track, menu, { menu = false }, vm, actions)
            }
        }
    }
}
