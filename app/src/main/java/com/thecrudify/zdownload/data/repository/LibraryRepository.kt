package com.thecrudify.zdownload.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.thecrudify.zdownload.data.db.DownloadEntity
import com.thecrudify.zdownload.data.db.FavoriteEntity
import com.thecrudify.zdownload.data.db.LibraryDao
import com.thecrudify.zdownload.data.db.PlaylistEntity
import com.thecrudify.zdownload.data.db.Track
import com.thecrudify.zdownload.data.db.TrackEntity
import com.thecrudify.zdownload.data.metadata.AudioMetadata
import com.thecrudify.zdownload.data.metadata.MetadataReader
import com.thecrudify.zdownload.data.metadata.TagEdit
import com.thecrudify.zdownload.data.metadata.TagWriter
import com.thecrudify.zdownload.data.metadata.UnreadableAudioException
import com.thecrudify.zdownload.data.storage.MusicStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.abs

class ImportResult(val imported: Int, val skipped: List<String>)
class VerifyResult(val checked: Int, val missing: Int)
class StorageStats(
    val musicPath: String,
    val trackCount: Int,
    val musicBytes: Long,
    val artworkBytes: Long,
    val tempBytes: Long,
    val freeBytes: Long,
    val totalBytes: Long
)

/** The music library: registers files, keeps metadata, playlists, favorites and history consistent. */
class LibraryRepository(
    private val context: Context,
    private val dao: LibraryDao,
    private val storage: MusicStorage
) {
    val tracks = dao.observeTracks()
    val playlists = dao.observePlaylists()
    fun playlistTracks(playlistId: Long) = dao.observePlaylistTracks(playlistId)

    suspend fun getTracks(ids: List<String>): List<Track> {
        val byId = ids.chunked(500).flatMap { dao.getTracks(it) }.associateBy { it.id }
        return ids.mapNotNull { byId[it] } // keep the caller's order
    }

    suspend fun getTrack(id: String): Track? = dao.getTrack(id)

    private suspend fun defaultStorageId() =
        dao.storageId("Musik aplikasi", storage.musicDir.absolutePath, System.currentTimeMillis())

    private fun clean(value: String?, fallback: String) =
        value?.replace(Regex("\\s+"), " ")?.trim()?.take(200)?.ifEmpty { null } ?: fallback

    private fun artworkExtension(bytes: ByteArray) =
        if (bytes.size > 4 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte()) "png" else "jpg"

    private fun scan(file: File) {
        // Makes the file show up in other music apps via MediaStore
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    // ---- Adding music -------------------------------------------------------------------

    /**
     * Moves a finished download into the music folder and (re)registers it.
     * A re-download of a song already in the library replaces its file and keeps the same
     * track id, so playlists, favorites and the user's metadata edits survive.
     */
    suspend fun registerDownload(
        download: DownloadEntity,
        audio: File,
        artwork: File?,
        meta: AudioMetadata
    ): Track = withContext(Dispatchers.IO) {
        val format = audio.extension.lowercase()
        val existing = dao.findTrackBySource(download.sourceId, format)
        val artworkBytes = artwork?.takeIf { it.isFile && it.length() in 1..MAX_ARTWORK_BYTES }?.readBytes() ?: meta.artwork

        if (existing != null) {
            val target = File(existing.entity.filePath)
            storage.commit(audio, target)
            val artworkPath = existing.entity.artworkPath
                ?: artworkBytes?.let { storage.saveArtwork(existing.id, it, artworkExtension(it)) }
            dao.updateTrack(
                existing.entity.copy(
                    durationMs = meta.durationMs, bitrate = meta.bitrate, sampleRate = meta.sampleRate,
                    codec = meta.mimeType, fileSize = target.length(), artworkPath = artworkPath, isMissing = false,
                    lyrics = meta.lyrics ?: existing.entity.lyrics
                )
            )
            scan(target)
            return@withContext dao.getTrack(existing.id)!!
        }

        val id = UUID.randomUUID().toString()
        val title = clean(download.title.ifBlank { meta.title }, "Unknown Track")
        val artist = clean(download.artist.ifBlank { meta.artist }, "Unknown Artist")
        val album = clean(meta.album, "")
        val target = storage.newTrackFile(artist, title, format)
        storage.commit(audio, target)
        try {
            dao.insertTrack(
                TrackEntity(
                    id = id, title = title,
                    artistId = dao.artistId(artist),
                    albumId = dao.albumId(album, clean(meta.albumArtist, artist)),
                    genre = clean(meta.genre, ""), year = meta.year,
                    trackNumber = meta.trackNumber, discNumber = meta.discNumber,
                    durationMs = meta.durationMs, bitrate = meta.bitrate, sampleRate = meta.sampleRate,
                    codec = meta.mimeType, format = format, fileSize = target.length(),
                    artworkPath = artworkBytes?.let { storage.saveArtwork(id, it, artworkExtension(it)) },
                    filePath = target.absolutePath, storageId = defaultStorageId(),
                    sourceId = download.sourceId, sourceUrl = download.sourceUrl,
                    dateAdded = System.currentTimeMillis(),
                    lyrics = meta.lyrics
                )
            )
        } catch (e: Exception) {
            // Don't leave a file on disk that the library doesn't know about
            target.delete()
            storage.deleteArtwork(id)
            throw e
        }
        scan(target)
        dao.getTrack(id)!!
    }

    /** Copies user-picked audio files into the music folder and registers them. Bad files are skipped with a reason. */
    suspend fun importAudio(uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        val skipped = mutableListOf<String>()
        var imported = 0
        val known = dao.getAllTracks()
        for (uri in uris) {
            var name = "file"
            var temp: File? = null
            try {
                var size = -1L
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                        c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
                    }
                }
                val mime = context.contentResolver.getType(uri).orEmpty()
                val extension = name.substringAfterLast('.', "").lowercase().ifEmpty {
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty()
                }
                if (extension !in IMPORT_EXTENSIONS) throw IOException("format .$extension tidak didukung")
                if (size == 0L) throw IOException("file kosong")
                if (size > MAX_IMPORT_BYTES) throw IOException("file terlalu besar")
                if (size > 0 && size + MIN_FREE_BYTES > storage.freeBytes()) throw IOException("penyimpanan tidak cukup")

                temp = File(storage.tempDir, "import-${UUID.randomUUID()}.$extension")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { input.copyTo(it) }
                } ?: throw IOException("file tidak bisa dibuka")

                val meta = MetadataReader.read(temp)
                val title = clean(meta.title, name.substringBeforeLast('.'))
                val artist = clean(meta.artist, "Unknown Artist")
                val duplicate = known.any {
                    it.title.equals(title, true) && it.artistName.equals(artist, true) &&
                            abs(it.entity.durationMs - meta.durationMs) < 1500 && it.entity.fileSize == temp.length()
                }
                if (duplicate) throw IOException("sudah ada di library")

                val id = UUID.randomUUID().toString()
                val target = storage.newTrackFile(artist, title, extension)
                storage.commit(temp, target)
                try {
                    dao.insertTrack(
                        TrackEntity(
                            id = id, title = title,
                            artistId = dao.artistId(artist),
                            albumId = dao.albumId(clean(meta.album, ""), clean(meta.albumArtist, artist)),
                            genre = clean(meta.genre, ""), year = meta.year,
                            trackNumber = meta.trackNumber, discNumber = meta.discNumber,
                            durationMs = meta.durationMs, bitrate = meta.bitrate, sampleRate = meta.sampleRate,
                            codec = meta.mimeType, format = extension, fileSize = target.length(),
                            artworkPath = meta.artwork?.takeIf { it.size <= MAX_ARTWORK_BYTES }
                                ?.let { storage.saveArtwork(id, it, artworkExtension(it)) },
                            filePath = target.absolutePath, storageId = defaultStorageId(),
                            sourceId = null, sourceUrl = null, dateAdded = System.currentTimeMillis(),
                            lyrics = meta.lyrics
                        )
                    )
                } catch (e: Exception) {
                    target.delete()
                    storage.deleteArtwork(id)
                    throw e
                }
                scan(target)
                imported++
            } catch (e: UnreadableAudioException) {
                skipped += "$name: ${e.message}"
            } catch (e: Exception) {
                skipped += "$name: ${e.message ?: "gagal diimpor"}"
            } finally {
                temp?.delete()
            }
        }
        ImportResult(imported, skipped)
    }

    // ---- Editing ------------------------------------------------------------------------

    /**
     * Saves edited metadata to the database, and into the file's own tags when asked and the
     * format allows it. The audio file is never moved or renamed by an edit.
     *
     * @return a note for the user when the file tags could not be written (the database edit still applies)
     */
    suspend fun updateMetadata(track: Track, edit: TagEdit, newArtwork: Uri?, writeToFile: Boolean): String? =
        withContext(Dispatchers.IO) {
            val title = clean(edit.title, track.title)
            val artist = clean(edit.artist, "Unknown Artist")
            val album = clean(edit.album, "")
            var artworkPath = track.entity.artworkPath
            if (newArtwork != null) {
                val bytes = context.contentResolver.openInputStream(newArtwork)?.use { it.readBytes() }
                if (bytes != null && bytes.size in 1..MAX_ARTWORK_BYTES) {
                    artworkPath = storage.saveArtwork(track.id, bytes, artworkExtension(bytes))
                }
            }
            dao.updateTrack(
                track.entity.copy(
                    title = title,
                    artistId = dao.artistId(artist),
                    albumId = dao.albumId(album, clean(edit.albumArtist, artist)),
                    genre = clean(edit.genre, ""), year = edit.year, trackNumber = edit.trackNumber,
                    artworkPath = artworkPath,
                    lyrics = edit.lyrics
                )
            )
            dao.pruneUnused()

            if (!writeToFile) return@withContext null
            val file = File(track.entity.filePath)
            if (!TagWriter.canWrite(track.entity.format)) {
                return@withContext "Format .${track.entity.format} tidak mendukung penulisan tag; perubahan disimpan di library aplikasi saja."
            }
            try {
                TagWriter.write(
                    file,
                    edit.copy(title = title, artist = artist, album = album),
                    artworkPath?.takeIf { newArtwork != null }?.let(::File)
                )
                dao.getTrack(track.id)?.let { dao.updateTrack(it.entity.copy(fileSize = file.length())) }
                scan(file)
                null
            } catch (e: Exception) {
                "Tag tidak tertulis ke file (${e.message}); perubahan tetap tersimpan di library aplikasi."
            }
        }

    suspend fun setFavorite(trackId: String, favorite: Boolean) {
        if (favorite) dao.addFavorite(FavoriteEntity(trackId, System.currentTimeMillis())) else dao.removeFavorite(trackId)
    }

    suspend fun recordPlay(trackId: String) = dao.recordPlay(trackId, System.currentTimeMillis())

    // ---- Removing -----------------------------------------------------------------------

    /**
     * Removes tracks from the library. The audio files are deleted only when [deleteFiles] is true.
     * @return how many files could not be deleted
     */
    suspend fun delete(tracks: List<Track>, deleteFiles: Boolean): Int = withContext(Dispatchers.IO) {
        dao.deleteTracks(tracks.map { it.id })
        var failed = 0
        for (track in tracks) {
            storage.deleteArtwork(track.id)
            if (deleteFiles) {
                val file = File(track.entity.filePath)
                if (file.exists() && !file.delete()) failed++
                // Tidy the artist folder if that was its last song
                file.parentFile?.takeIf { it != storage.musicDir && it.list()?.isEmpty() == true }?.delete()
            }
        }
        failed
    }

    // ---- Playlists ----------------------------------------------------------------------

    suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = clean(name, "Playlist"), createdAt = System.currentTimeMillis()))

    suspend fun renamePlaylist(id: Long, name: String) = dao.renamePlaylist(id, clean(name, "Playlist"))
    suspend fun deletePlaylist(id: Long) = dao.deletePlaylist(id)
    suspend fun addToPlaylist(playlistId: Long, trackIds: List<String>) = dao.addToPlaylist(playlistId, trackIds)
    suspend fun removeFromPlaylist(playlistId: Long, trackId: String) = dao.removeFromPlaylist(playlistId, trackId)
    suspend fun setPlaylistOrder(playlistId: Long, trackIds: List<String>) = dao.setPlaylistOrder(playlistId, trackIds)

    // ---- Storage management -------------------------------------------------------------

    /** Flags tracks whose file is gone (and un-flags ones that came back). Nothing is deleted. */
    suspend fun verify(): VerifyResult = withContext(Dispatchers.IO) {
        val all = dao.getAllTracks()
        var missing = 0
        for (track in all) {
            val file = File(track.entity.filePath)
            val isMissing = !file.isFile || file.length() == 0L
            if (isMissing) missing++
            if (isMissing != track.entity.isMissing) dao.setMissing(track.id, isMissing)
        }
        VerifyResult(all.size, missing)
    }

    suspend fun markMissing(trackId: String) = dao.setMissing(trackId, true)

    suspend fun stats(): StorageStats = withContext(Dispatchers.IO) {
        val all = dao.getAllTracks()
        StorageStats(
            musicPath = storage.musicDir.absolutePath,
            trackCount = all.size,
            musicBytes = all.filter { !it.entity.isMissing }.sumOf { it.entity.fileSize },
            artworkBytes = storage.sizeOf(storage.artworkDir),
            tempBytes = storage.sizeOf(storage.tempDir),
            freeBytes = storage.freeBytes(),
            totalBytes = storage.totalBytes()
        )
    }

    suspend fun clearTemp(activeWorkDirs: Set<String>): Long =
        withContext(Dispatchers.IO) { storage.clearTemp(activeWorkDirs) }

    /** Copies tracks into a folder the user picked with the system folder picker. Returns how many were written. */
    suspend fun export(tracks: List<Track>, treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        var done = 0
        for (track in tracks) {
            val file = File(track.entity.filePath)
            if (!file.isFile) continue
            runCatching {
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "audio/*"
                val doc = DocumentsContract.createDocument(context.contentResolver, parent, mime, file.name)
                    ?: throw IOException("tidak bisa membuat file")
                context.contentResolver.openOutputStream(doc)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                    ?: throw IOException("tidak bisa menulis file")
                done++
            }
        }
        done
    }

    companion object {
        val IMPORT_EXTENSIONS = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus")
        const val MAX_IMPORT_BYTES = 1024L * 1024 * 1024
        const val MAX_ARTWORK_BYTES = 10L * 1024 * 1024
        /** Below this much free space, downloads and imports refuse to start. */
        const val MIN_FREE_BYTES = 100L * 1024 * 1024
    }
}
