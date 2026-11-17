package com.thecrudify.zdownload.viewmodel

import com.thecrudify.zdownload.ui.t
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thecrudify.zdownload.ZDownloadApp
import com.thecrudify.zdownload.data.db.DownloadEntity
import com.thecrudify.zdownload.data.db.PlaylistWithCount
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.download.LiveProgress
import com.thecrudify.zdownload.data.library.LibraryQuery
import com.thecrudify.zdownload.data.library.LibrarySection
import com.thecrudify.zdownload.data.library.TrackGroup
import com.thecrudify.zdownload.data.library.TrackSort
import com.thecrudify.zdownload.data.metadata.TagEdit
import com.thecrudify.zdownload.data.repository.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadItem(val download: DownloadEntity, val progress: LiveProgress?)

/** State and actions for everything outside Home: library, playlists, downloads list, storage. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ZDownloadApp
    private val library = app.library
    val playback = app.playback

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** One-off notices for the snackbar (results of actions, playback errors). */
    val messages: SharedFlow<String> = _messages

    private fun <T> kotlinx.coroutines.flow.Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val themeMode: StateFlow<com.thecrudify.zdownload.data.prefs.ThemeMode> =
        app.prefs.themeMode.state(com.thecrudify.zdownload.data.prefs.ThemeMode.SYSTEM)
    fun setThemeMode(mode: com.thecrudify.zdownload.data.prefs.ThemeMode) =
        viewModelScope.launch { app.prefs.setThemeMode(mode) }

    val language: StateFlow<String> = app.prefs.language.state("SYSTEM")
    fun setLanguage(code: String) = viewModelScope.launch { app.prefs.setLanguage(code) }

    val defaultSpeed: StateFlow<Float> = app.prefs.defaultSpeed.state(1f)
    fun setDefaultSpeed(speed: Float) = viewModelScope.launch { app.prefs.setDefaultSpeed(speed) }

    val defaultRepeat: StateFlow<Int> = app.prefs.defaultRepeat.state(0)
    fun setDefaultRepeat(repeat: Int) = viewModelScope.launch { app.prefs.setDefaultRepeat(repeat) }

    val defaultShuffle: StateFlow<Boolean> = app.prefs.defaultShuffle.state(false)
    fun setDefaultShuffle(shuffle: Boolean) = viewModelScope.launch { app.prefs.setDefaultShuffle(shuffle) }

    // ---- Library ------------------------------------------------------------------------

    val allTracks: StateFlow<List<Track>> = library.tracks.state(emptyList())

    val section = MutableStateFlow(LibrarySection.SONGS)
    val query = MutableStateFlow("")
    val sort = MutableStateFlow(TrackSort.DATE_ADDED)
    /** The album / artist / genre currently opened inside a grouped section. */
    val group = MutableStateFlow<String?>(null)
    val selection = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch { app.playback.errors.collect { _messages.emit(it) } }
        viewModelScope.launch {
            sort.value = runCatching { TrackSort.valueOf(app.prefs.librarySort.first()) }.getOrDefault(TrackSort.DATE_ADDED)
        }
    }

    fun setSection(value: LibrarySection) {
        section.value = value
        group.value = null
        selection.value = emptySet()
    }

    fun setSort(value: TrackSort) {
        sort.value = value
        viewModelScope.launch { app.prefs.setLibrarySort(value.name) }
    }

    // Typing shouldn't re-filter thousands of rows on every keystroke
    private val debouncedQuery = query.debounce(200)

    val visibleTracks: StateFlow<List<Track>> =
        combine(library.tracks, section, debouncedQuery, sort, group) { all, section, query, sort, group ->
            LibraryQuery.tracks(all, section, query, sort, group)
        }.flowOn(Dispatchers.Default).state(emptyList())

    val groups: StateFlow<List<TrackGroup>> =
        combine(library.tracks, section, debouncedQuery) { all, section, query ->
            if (section.isGrouped) LibraryQuery.groups(all, section, query) else emptyList()
        }.flowOn(Dispatchers.Default).state(emptyList())

    fun toggleSelected(trackId: String) {
        selection.value = selection.value.let { if (trackId in it) it - trackId else it + trackId }
    }

    fun selectAll(tracks: List<Track>) {
        selection.value = tracks.map { it.id }.toSet()
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun selectedTracks(): List<Track> = allTracks.value.filter { it.id in selection.value }

    // ---- Playback shortcuts -------------------------------------------------------------

    fun play(tracks: List<Track>, index: Int = 0, shuffle: Boolean? = null) = playback.play(tracks, index, shuffle)

    fun playNext(tracks: List<Track>) {
        playback.playNext(tracks)
        notify(t("Diputar berikutnya", "Playing next"))
    }

    fun addToQueue(tracks: List<Track>) {
        playback.addToQueue(tracks)
        notify(t("Ditambahkan ke antrean", "Added to queue"))
    }

    fun toggleFavorite(track: Track) = viewModelScope.launch { library.setFavorite(track.id, !track.isFavorite) }

    fun toggleFavorite(trackId: String) = viewModelScope.launch {
        library.getTrack(trackId)?.let { library.setFavorite(it.id, !it.isFavorite) }
    }

    // ---- Editing & removing -------------------------------------------------------------

    fun saveMetadata(track: Track, edit: TagEdit, artwork: Uri?, writeToFile: Boolean) = viewModelScope.launch {
        val note = runCatching { library.updateMetadata(track, edit, artwork, writeToFile) }
            .getOrElse { t("Gagal menyimpan: ", "Couldn't save: ") + it.message }
        library.getTrack(track.id)?.let(playback::refreshTrack)
        notify(note ?: if (writeToFile) t("Info lagu disimpan, termasuk ke file", "Song info saved, including to the file") else t("Info lagu disimpan", "Song info saved"))
    }

    fun delete(tracks: List<Track>, deleteFiles: Boolean) = viewModelScope.launch {
        playback.removeTracks(tracks.map { it.id }.toSet())
        val failed = library.delete(tracks, deleteFiles)
        clearSelection()
        notify(
            when {
                failed > 0 -> t("${tracks.size} lagu dihapus dari library; $failed file tidak bisa dihapus", "${songs(tracks.size)} removed from library; $failed file(s) couldn't be deleted")
                deleteFiles -> t("${tracks.size} lagu dan filenya dihapus", "${songs(tracks.size)} and their files deleted")
                else -> t("${tracks.size} lagu dihapus dari library (file tetap ada)", "${songs(tracks.size)} removed from library (files kept)")
            }
        )
    }

    fun importAudio(uris: List<Uri>) = viewModelScope.launch {
        if (uris.isEmpty()) return@launch
        notify(t("Mengimpor ${uris.size} file…", "Importing ${uris.size} file(s)…"))
        val result = library.importAudio(uris)
        notify(
            buildString {
                append(t("${result.imported} lagu diimpor", "${songs(result.imported)} imported"))
                if (result.skipped.isNotEmpty()) append(t("; ${result.skipped.size} dilewati: ", "; ${result.skipped.size} skipped: ") + result.skipped.first())
            }
        )
        refreshStorage()
    }

    fun export(tracks: List<Track>, folder: Uri) = viewModelScope.launch {
        val done = library.export(tracks, folder)
        clearSelection()
        notify(t("$done dari ${tracks.size} lagu disalin ke folder yang dipilih", "$done of ${songs(tracks.size)} copied to the chosen folder"))
    }

    // ---- Playlists ----------------------------------------------------------------------

    val playlists: StateFlow<List<PlaylistWithCount>> = library.playlists.state(emptyList())
    val openPlaylistId = MutableStateFlow<Long?>(null)
    val playlistTracks: StateFlow<List<Track>> =
        openPlaylistId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else library.playlistTracks(id) }
            .state(emptyList())

    fun createPlaylist(name: String, trackIds: List<String> = emptyList()) = viewModelScope.launch {
        val id = library.createPlaylist(name)
        if (trackIds.isNotEmpty()) {
            library.addToPlaylist(id, trackIds)
            clearSelection()
            notify(t("${trackIds.size} lagu ditambahkan ke \"$name\"", "${songs(trackIds.size)} added to \"$name\""))
        }
    }

    fun addToPlaylist(playlist: PlaylistWithCount, trackIds: List<String>) = viewModelScope.launch {
        val added = library.addToPlaylist(playlist.playlist.id, trackIds)
        clearSelection()
        notify(if (added == 0) t("Sudah ada di \"${playlist.playlist.name}\"", "Already in \"${playlist.playlist.name}\"") else t("$added lagu ditambahkan ke \"${playlist.playlist.name}\"", "${songs(added)} added to \"${playlist.playlist.name}\""))
    }

    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch { library.renamePlaylist(id, name) }

    fun deletePlaylist(id: Long) = viewModelScope.launch {
        library.deletePlaylist(id)
        openPlaylistId.value = null
    }

    fun removeFromPlaylist(playlistId: Long, trackId: String) =
        viewModelScope.launch { library.removeFromPlaylist(playlistId, trackId) }

    fun movePlaylistTrack(playlistId: Long, from: Int, to: Int) = viewModelScope.launch {
        val ids = playlistTracks.value.map { it.id }.toMutableList()
        if (from !in ids.indices || to !in ids.indices) return@launch
        ids.add(to, ids.removeAt(from))
        library.setPlaylistOrder(playlistId, ids)
    }

    // ---- Downloads ----------------------------------------------------------------------

    val downloads: StateFlow<List<DownloadItem>> =
        combine(app.database.dao().observeDownloads(), app.downloads.live) { all, live ->
            all.map { DownloadItem(it, live[it.id]) }
        }.state(emptyList())

    fun pauseDownload(id: Long) = viewModelScope.launch { app.downloads.pause(id) }
    fun cancelDownload(id: Long) = viewModelScope.launch { app.downloads.cancel(id) }
    fun retryDownload(id: Long) = viewModelScope.launch { app.downloads.retry(id) }
    fun removeDownload(id: Long) = viewModelScope.launch { app.downloads.remove(id) }
    fun clearFinishedDownloads() = viewModelScope.launch { app.downloads.clearFinished() }

    fun playDownloaded(trackId: String) = viewModelScope.launch {
        library.getTrack(trackId)?.let { playback.play(listOf(it)) } ?: notify(t("Lagu sudah tidak ada di library", "That song is no longer in the library"))
    }

    // ---- Storage ------------------------------------------------------------------------

    val storageStats = MutableStateFlow<StorageStats?>(null)

    fun refreshStorage() = viewModelScope.launch { storageStats.value = library.stats() }

    fun verifyLibrary() = viewModelScope.launch {
        val result = library.verify()
        refreshStorage()
        notify(
            if (result.missing == 0) t("${result.checked} lagu diperiksa, semua file ada", "${songs(result.checked)} checked, all files present")
            else t("${result.missing} dari ${result.checked} lagu filenya hilang (ditandai, tidak dihapus)", "${result.missing} of ${songs(result.checked)} have missing files (marked, not removed)")
        )
    }

    fun clearTemp() = viewModelScope.launch {
        val freed = library.clearTemp(app.downloads.workDirsInUse())
        refreshStorage()
        notify(t("File sementara dibersihkan (${freed / 1_048_576} MB)", "Temporary files cleared (${freed / 1_048_576} MB)"))
    }

    private fun notify(message: String) {
        _messages.tryEmit(message)
    }
}

/** English count noun for notifications: "1 song", "3 songs". */
private fun songs(n: Int) = if (n == 1) "1 song" else "$n songs"
