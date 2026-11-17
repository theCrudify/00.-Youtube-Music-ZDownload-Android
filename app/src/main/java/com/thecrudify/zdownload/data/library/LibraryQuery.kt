package com.thecrudify.zdownload.data.library

import com.thecrudify.zdownload.data.db.Track

enum class LibrarySection { SONGS, ALBUMS, ARTISTS, GENRES, FAVORITES, RECENTLY_ADDED, RECENTLY_PLAYED, DOWNLOADED;
    val isGrouped: Boolean get() = this == ALBUMS || this == ARTISTS || this == GENRES
}

enum class TrackSort { DATE_ADDED, LAST_PLAYED, TITLE, ARTIST, ALBUM, DURATION, FILE_SIZE, YEAR }

data class TrackGroup(val name: String, val trackCount: Int, val artworkPath: String?)

/** Search, filter, sort and grouping for the library. Pure functions over the in-memory track list. */
object LibraryQuery {

    const val NO_ALBUM = "(Tanpa album)"
    const val NO_GENRE = "(Tanpa genre)"
    private const val RECENT_LIMIT = 50

    fun groupKey(track: Track, section: LibrarySection): String = when (section) {
        LibrarySection.ALBUMS -> track.album.ifBlank { NO_ALBUM }
        LibrarySection.GENRES -> track.entity.genre.ifBlank { NO_GENRE }
        else -> track.artistName
    }

    private fun matches(track: Track, query: String): Boolean =
        query.isBlank() || listOf(track.title, track.artistName, track.album, track.entity.genre)
            .any { it.contains(query.trim(), ignoreCase = true) }

    /**
     * @param group for ALBUMS/ARTISTS/GENRES: the album/artist/genre that was opened (null lists nothing; show [groups] instead)
     */
    fun tracks(all: List<Track>, section: LibrarySection, query: String, sort: TrackSort, group: String? = null): List<Track> {
        val searched = all.filter { matches(it, query) }
        return when (section) {
            // These two sections are defined by their order, so the sort setting does not apply
            LibrarySection.RECENTLY_ADDED -> searched.sortedByDescending { it.entity.dateAdded }.take(RECENT_LIMIT)
            LibrarySection.RECENTLY_PLAYED -> searched.filter { it.entity.lastPlayedAt != null }
                .sortedByDescending { it.entity.lastPlayedAt }.take(RECENT_LIMIT)
            LibrarySection.FAVORITES -> sort(searched.filter { it.isFavorite }, sort)
            LibrarySection.DOWNLOADED -> sort(searched.filter { it.entity.sourceId != null }, sort)
            LibrarySection.SONGS -> sort(searched, sort)
            LibrarySection.ALBUMS, LibrarySection.ARTISTS, LibrarySection.GENRES ->
                if (group == null) emptyList()
                else if (section == LibrarySection.ALBUMS) {
                    // Inside an album the natural order is disc/track number
                    all.filter { groupKey(it, section) == group }
                        .sortedWith(compareBy({ it.entity.discNumber ?: 0 }, { it.entity.trackNumber ?: Int.MAX_VALUE }, { it.title.lowercase() }))
                } else sort(all.filter { groupKey(it, section) == group }, sort)
        }
    }

    fun sort(tracks: List<Track>, sort: TrackSort): List<Track> = when (sort) {
        TrackSort.DATE_ADDED -> tracks.sortedByDescending { it.entity.dateAdded }
        TrackSort.LAST_PLAYED -> tracks.sortedByDescending { it.entity.lastPlayedAt ?: 0 }
        TrackSort.TITLE -> tracks.sortedBy { it.title.lowercase() }
        TrackSort.ARTIST -> tracks.sortedWith(compareBy({ it.artistName.lowercase() }, { it.title.lowercase() }))
        TrackSort.ALBUM -> tracks.sortedWith(compareBy({ it.album.lowercase() }, { it.entity.trackNumber ?: Int.MAX_VALUE }, { it.title.lowercase() }))
        TrackSort.DURATION -> tracks.sortedByDescending { it.entity.durationMs }
        TrackSort.FILE_SIZE -> tracks.sortedByDescending { it.entity.fileSize }
        TrackSort.YEAR -> tracks.sortedByDescending { it.entity.year ?: 0 }
    }

    fun groups(all: List<Track>, section: LibrarySection, query: String): List<TrackGroup> =
        all.groupBy { groupKey(it, section) }
            .filterKeys { query.isBlank() || it.contains(query.trim(), ignoreCase = true) }
            .map { (name, tracks) -> TrackGroup(name, tracks.size, tracks.firstNotNullOfOrNull { it.entity.artworkPath }) }
            .sortedBy { it.name.lowercase() }
}
