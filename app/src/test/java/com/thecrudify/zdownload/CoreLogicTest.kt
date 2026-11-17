package com.thecrudify.zdownload

import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.db.TrackEntity
import com.thecrudify.zdownload.data.download.YtDlp
import com.thecrudify.zdownload.data.library.LibraryQuery
import com.thecrudify.zdownload.data.library.LibrarySection
import com.thecrudify.zdownload.data.library.TrackSort
import com.thecrudify.zdownload.data.model.AudioFormat
import com.thecrudify.zdownload.data.storage.safeFileName
import com.thecrudify.zdownload.ui.components.formatBytes
import com.thecrudify.zdownload.ui.components.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    // ---- 1. YtDlp Helper Tests -----------------------------------------------------------

    @Test
    fun ytDlp_isSupportedUrl_validUrls() {
        assertTrue(YtDlp.isSupportedUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YtDlp.isSupportedUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(YtDlp.isSupportedUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YtDlp.isSupportedUrl("http://youtube.com/shorts/abcd1234efg"))
    }

    @Test
    fun ytDlp_isSupportedUrl_invalidUrls() {
        assertFalse(YtDlp.isSupportedUrl("https://facebook.com/video"))
        assertFalse(YtDlp.isSupportedUrl("https://spotify.com/track/123"))
        assertFalse(YtDlp.isSupportedUrl("invalid-url-string"))
        assertFalse(YtDlp.isSupportedUrl(""))
    }

    @Test
    fun ytDlp_parseSpeed() {
        assertEquals("1.20MiB/s", YtDlp.parseSpeed("[download]  42.0% of 3.27MiB at 1.20MiB/s ETA 00:02"))
        assertEquals("350.5KiB/s", YtDlp.parseSpeed("[download]  10.0% of 10.00MiB at 350.5 KiB/s ETA 00:15"))
        assertNull(YtDlp.parseSpeed("Just some arbitrary log line"))
    }

    @Test
    fun ytDlp_summarizeError() {
        val errorLog = """
            [youtube] Extracting URL: https://youtu.be/test
            WARNING: could not extract subtitles
            ERROR: Video unavailable
            This video is not available in your country
        """.trimIndent()
        val summary = YtDlp.summarizeError(errorLog)
        assertTrue(summary.contains("ERROR: Video unavailable"))
    }

    // ---- 2. Storage & Filename Tests -----------------------------------------------------

    @Test
    fun storage_safeFileName_cleansIllegalCharacters() {
        assertEquals("Song Title", safeFileName("Song: Title?"))
        assertEquals("Artist Name Remix", safeFileName("Artist / Name <Remix>"))
        assertEquals("Normal Track", safeFileName("Normal Track"))
    }

    @Test
    fun storage_safeFileName_fallback() {
        assertEquals("Unknown", safeFileName("???///:::"))
        assertEquals("Fallback", safeFileName("", fallback = "Fallback"))
    }

    // ---- 3. Formatting Tests -------------------------------------------------------------

    @Test
    fun formatting_duration() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:45", formatDuration(45_000L))
        assertEquals("3:25", formatDuration(205_000L))
        assertEquals("1:05:00", formatDuration(3_900_000L))
    }

    @Test
    fun formatting_bytes() {
        assertEquals("500 KB", formatBytes(500 * 1024L))
        assertEquals("15.0 MB", formatBytes(15 * 1024 * 1024L))
        assertEquals("1.5 GB", formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    // ---- 4. AudioFormat Tests ------------------------------------------------------------

    @Test
    fun audioFormats_checkAllValues() {
        val formats = AudioFormat.entries
        assertTrue(formats.contains(AudioFormat.ORIGINAL))
        assertTrue(formats.contains(AudioFormat.MP3))
        assertTrue(formats.contains(AudioFormat.M4A))
        assertTrue(formats.contains(AudioFormat.FLAC))
        assertTrue(formats.contains(AudioFormat.WAV))
        assertTrue(formats.contains(AudioFormat.OPUS))

        assertEquals("mp3", AudioFormat.MP3.extension)
        assertEquals("flac", AudioFormat.FLAC.extension)
        assertEquals("m4a", AudioFormat.M4A.extension)
        assertEquals("wav", AudioFormat.WAV.extension)
        assertEquals("opus", AudioFormat.OPUS.extension)
    }

    // ---- 5. LibraryQuery Tests -----------------------------------------------------------

    private fun mockTrack(
        id: String,
        title: String,
        artist: String,
        album: String = "",
        genre: String = "",
        durationMs: Long = 180_000L,
        fileSize: Long = 5_000_000L,
        isFavorite: Boolean = false,
        sourceId: String? = "src-$id",
        dateAdded: Long = 1000L,
        lyrics: String? = null
    ): Track {
        return Track(
            entity = TrackEntity(
                id = id,
                title = title,
                artistId = 1L,
                albumId = if (album.isNotEmpty()) 1L else null,
                genre = genre,
                year = 2024,
                trackNumber = 1,
                discNumber = 1,
                durationMs = durationMs,
                bitrate = 320,
                sampleRate = 44100,
                codec = "audio/mpeg",
                format = "mp3",
                fileSize = fileSize,
                artworkPath = null,
                filePath = "/music/$artist/$title.mp3",
                storageId = 1L,
                sourceId = sourceId,
                sourceUrl = "https://youtube.com/watch?v=$id",
                dateAdded = dateAdded,
                lastPlayedAt = null,
                playCount = 0,
                isMissing = false,
                lyrics = lyrics
            ),
            artistName = artist,
            albumTitle = album.ifEmpty { null },
            albumArtist = if (album.isNotEmpty()) artist else null,
            isFavorite = isFavorite
        )
    }

    @Test
    fun libraryQuery_tracksSearch() {
        val tracks = listOf(
            mockTrack("1", "Bohemian Rhapsody", "Queen", "A Night at the Opera", "Rock"),
            mockTrack("2", "Shape of You", "Ed Sheeran", "Divide", "Pop"),
            mockTrack("3", "Radio Ga Ga", "Queen", "The Works", "Rock")
        )

        val searchResult = LibraryQuery.tracks(tracks, LibrarySection.SONGS, "Queen", TrackSort.TITLE)
        assertEquals(2, searchResult.size)
        assertEquals("Bohemian Rhapsody", searchResult[0].title)
        assertEquals("Radio Ga Ga", searchResult[1].title)

        val genreResult = LibraryQuery.tracks(tracks, LibrarySection.SONGS, "Pop", TrackSort.TITLE)
        assertEquals(1, genreResult.size)
        assertEquals("Shape of You", genreResult[0].title)
    }

    @Test
    fun libraryQuery_sorting() {
        val tracks = listOf(
            mockTrack("1", "Bravo", "Artist A", durationMs = 100_000L, fileSize = 2_000_000L),
            mockTrack("2", "Alpha", "Artist B", durationMs = 300_000L, fileSize = 1_000_000L),
            mockTrack("3", "Charlie", "Artist C", durationMs = 200_000L, fileSize = 3_000_000L)
        )

        val byTitle = LibraryQuery.sort(tracks, TrackSort.TITLE)
        assertEquals("Alpha", byTitle[0].title)
        assertEquals("Bravo", byTitle[1].title)
        assertEquals("Charlie", byTitle[2].title)

        val byDuration = LibraryQuery.sort(tracks, TrackSort.DURATION)
        assertEquals("Alpha", byDuration[0].title)
        assertEquals("Charlie", byDuration[1].title)
        assertEquals("Bravo", byDuration[2].title)

        val bySize = LibraryQuery.sort(tracks, TrackSort.FILE_SIZE)
        assertEquals("Charlie", bySize[0].title)
        assertEquals("Bravo", bySize[1].title)
        assertEquals("Alpha", bySize[2].title)
    }

    @Test
    fun libraryQuery_favoritesAndDownloaded() {
        val tracks = listOf(
            mockTrack("1", "Song 1", "Artist 1", isFavorite = true, sourceId = "src-1"),
            mockTrack("2", "Song 2", "Artist 2", isFavorite = false, sourceId = null),
            mockTrack("3", "Song 3", "Artist 3", isFavorite = true, sourceId = "src-3")
        )

        val favorites = LibraryQuery.tracks(tracks, LibrarySection.FAVORITES, "", TrackSort.TITLE)
        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.isFavorite })

        val downloaded = LibraryQuery.tracks(tracks, LibrarySection.DOWNLOADED, "", TrackSort.TITLE)
        assertEquals(2, downloaded.size)
        assertTrue(downloaded.all { it.entity.sourceId != null })
    }

    @Test
    fun libraryQuery_groups() {
        val tracks = listOf(
            mockTrack("1", "Song 1", "Queen", "Album A"),
            mockTrack("2", "Song 2", "Queen", "Album B"),
            mockTrack("3", "Song 3", "Coldplay", "Album C")
        )

        val artistGroups = LibraryQuery.groups(tracks, LibrarySection.ARTISTS, "")
        assertEquals(2, artistGroups.size)
        val queenGroup = artistGroups.first { it.name == "Queen" }
        assertEquals(2, queenGroup.trackCount)
    }

    @Test
    fun track_lyricsStorage() {
        val track = mockTrack("1", "Song with lyrics", "Artist", lyrics = "Hello, world!\nLine 2")
        assertNotNull(track.entity.lyrics)
        assertEquals("Hello, world!\nLine 2", track.entity.lyrics)
    }
}
