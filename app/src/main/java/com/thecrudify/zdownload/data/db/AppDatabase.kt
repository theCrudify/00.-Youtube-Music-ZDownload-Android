package com.thecrudify.zdownload.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

private const val TRACK_SELECT = """
    SELECT t.*, ar.name AS artistName, al.title AS albumTitle, al.albumArtist AS albumArtist,
           EXISTS(SELECT 1 FROM favorites f WHERE f.trackId = t.id) AS isFavorite
    FROM tracks t
    JOIN artists ar ON ar.id = t.artistId
    LEFT JOIN albums al ON al.id = t.albumId
"""

@Dao
abstract class LibraryDao {

    // ---- Tracks -------------------------------------------------------------------------

    @Query("$TRACK_SELECT ORDER BY t.dateAdded DESC")
    abstract fun observeTracks(): Flow<List<Track>>

    @Query("$TRACK_SELECT WHERE t.id = :id")
    abstract suspend fun getTrack(id: String): Track?

    @Query("$TRACK_SELECT WHERE t.id IN (:ids)")
    abstract suspend fun getTracks(ids: List<String>): List<Track>

    @Query("$TRACK_SELECT")
    abstract suspend fun getAllTracks(): List<Track>

    @Query("$TRACK_SELECT WHERE t.sourceId = :sourceId AND t.format = :format")
    abstract suspend fun findTrackBySource(sourceId: String, format: String): Track?

    @Insert
    abstract suspend fun insertTrack(track: TrackEntity)

    @Update
    abstract suspend fun updateTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    protected abstract suspend fun deleteTrackRows(ids: List<String>)

    @Query("UPDATE tracks SET isMissing = :missing WHERE id = :id")
    abstract suspend fun setMissing(id: String, missing: Boolean)

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM tracks WHERE isMissing = 0")
    abstract fun observeLibraryBytes(): Flow<Long>

    // ---- Artists & albums ---------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertArtist(artist: ArtistEntity): Long

    @Query("SELECT id FROM artists WHERE name = :name")
    protected abstract suspend fun findArtistId(name: String): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertAlbum(album: AlbumEntity): Long

    @Query("SELECT id FROM albums WHERE title = :title AND albumArtist = :albumArtist")
    protected abstract suspend fun findAlbumId(title: String, albumArtist: String): Long

    @Transaction
    open suspend fun artistId(name: String): Long {
        insertArtist(ArtistEntity(name = name))
        return findArtistId(name)
    }

    /** Null when the track has no album. */
    @Transaction
    open suspend fun albumId(title: String, albumArtist: String): Long? {
        if (title.isBlank()) return null
        insertAlbum(AlbumEntity(title = title, albumArtist = albumArtist))
        return findAlbumId(title, albumArtist)
    }

    @Query("DELETE FROM albums WHERE id NOT IN (SELECT albumId FROM tracks WHERE albumId IS NOT NULL)")
    protected abstract suspend fun deleteUnusedAlbums()

    @Query("DELETE FROM artists WHERE id NOT IN (SELECT artistId FROM tracks)")
    protected abstract suspend fun deleteUnusedArtists()

    /** Artists/albums exist only while a track uses them. */
    @Transaction
    open suspend fun pruneUnused() {
        deleteUnusedAlbums()
        deleteUnusedArtists()
    }

    /** Removes the rows; playlist links, favorites and history go with them (cascade). */
    @Transaction
    open suspend fun deleteTracks(ids: List<String>) {
        // SQLite caps bound parameters (999 on old versions)
        ids.chunked(500).forEach { deleteTrackRows(it) }
        pruneUnused()
    }

    // ---- Favorites & history ------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    abstract suspend fun removeFavorite(trackId: String)

    @Insert
    protected abstract suspend fun insertHistory(entry: PlaybackHistoryEntity)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :now WHERE id = :trackId")
    protected abstract suspend fun bumpPlayCount(trackId: String, now: Long)

    @Query("DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY playedAt DESC LIMIT 1000)")
    protected abstract suspend fun trimHistory()

    @Transaction
    open suspend fun recordPlay(trackId: String, now: Long) {
        bumpPlayCount(trackId, now)
        insertHistory(PlaybackHistoryEntity(trackId = trackId, playedAt = now))
        trimHistory()
    }

    // ---- Playlists ----------------------------------------------------------------------

    @Query(
        """SELECT p.*, COUNT(pt.trackId) AS trackCount FROM playlists p
           LEFT JOIN playlist_tracks pt ON pt.playlistId = p.id
           GROUP BY p.id ORDER BY p.name COLLATE NOCASE"""
    )
    abstract fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query("$TRACK_SELECT JOIN playlist_tracks pt ON pt.trackId = t.id WHERE pt.playlistId = :playlistId ORDER BY pt.position")
    abstract fun observePlaylistTracks(playlistId: Long): Flow<List<Track>>

    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPlaylistTrack(link: PlaylistTrackEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    protected abstract suspend fun maxPosition(playlistId: Long): Int

    /** Appends tracks; ones already in the playlist are skipped. Returns how many were added. */
    @Transaction
    open suspend fun addToPlaylist(playlistId: Long, trackIds: List<String>): Int {
        var position = maxPosition(playlistId)
        var added = 0
        for (trackId in trackIds) {
            if (insertPlaylistTrack(PlaylistTrackEntity(playlistId, trackId, position + 1)) != -1L) {
                position++
                added++
            }
        }
        return added
    }

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    abstract suspend fun removeFromPlaylist(playlistId: Long, trackId: String)

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    protected abstract suspend fun setPosition(playlistId: Long, trackId: String, position: Int)

    @Transaction
    open suspend fun setPlaylistOrder(playlistId: Long, orderedTrackIds: List<String>) {
        orderedTrackIds.forEachIndexed { index, trackId -> setPosition(playlistId, trackId, index) }
    }

    // ---- Downloads ----------------------------------------------------------------------

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    abstract fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    abstract fun observeDownload(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE id = :id")
    abstract suspend fun getDownload(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE sourceId = :sourceId AND format = :format")
    abstract suspend fun findDownload(sourceId: String, format: String): DownloadEntity?

    @Insert
    abstract suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    abstract suspend fun updateDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    abstract suspend fun deleteDownload(id: Long)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    abstract suspend fun deleteFinishedDownloads()

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING', 'VERIFYING', 'EXTRACTING_METADATA', 'REGISTERING_LIBRARY')")
    abstract suspend fun getActiveDownloads(): List<DownloadEntity>

    // ---- Storage locations --------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertStorage(storage: StorageEntity): Long

    @Query("SELECT id FROM storage_locations WHERE path = :path")
    protected abstract suspend fun findStorageId(path: String): Long

    @Transaction
    open suspend fun storageId(label: String, path: String, now: Long): Long {
        insertStorage(StorageEntity(label = label, path = path, isDefault = true, createdAt = now))
        return findStorageId(path)
    }
}

@Database(
    entities = [
        StorageEntity::class, ArtistEntity::class, AlbumEntity::class, TrackEntity::class,
        FavoriteEntity::class, PlaybackHistoryEntity::class, PlaylistEntity::class,
        PlaylistTrackEntity::class, DownloadEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): LibraryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN lyrics TEXT DEFAULT NULL")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "library.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
