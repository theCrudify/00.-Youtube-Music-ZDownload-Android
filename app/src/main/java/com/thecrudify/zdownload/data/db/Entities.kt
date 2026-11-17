package com.thecrudify.zdownload.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A root folder that holds audio files. Tracks point at one so usage can be reported per location. */
@Entity(tableName = "storage_locations", indices = [Index(value = ["path"], unique = true)])
data class StorageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val path: String,
    val isDefault: Boolean,
    val createdAt: Long
)

@Entity(tableName = "artists", indices = [Index(value = ["name"], unique = true)])
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "albums", indices = [Index(value = ["title", "albumArtist"], unique = true)])
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val albumArtist: String
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artistId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AlbumEntity::class, parentColumns = ["id"], childColumns = ["albumId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = StorageEntity::class, parentColumns = ["id"], childColumns = ["storageId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index("artistId"), Index("albumId"), Index("storageId"),
        Index("title"), Index("dateAdded"), Index("lastPlayedAt"),
        Index(value = ["filePath"], unique = true),
        // One library entry per source + format: a retried download updates it instead of duplicating
        Index(value = ["sourceId", "format"], unique = true)
    ]
)
data class TrackEntity(
    /** Stable UUID: playlists, the play queue and the media session all refer to tracks by this. */
    @PrimaryKey val id: String,
    val title: String,
    val artistId: Long,
    val albumId: Long?,
    val genre: String,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long,
    val bitrate: Int?,
    val sampleRate: Int?,
    val codec: String,
    val format: String,
    val fileSize: Long,
    val artworkPath: String?,
    val filePath: String,
    val storageId: Long,
    val sourceId: String?,
    val sourceUrl: String?,
    val dateAdded: Long,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0,
    val isMissing: Boolean = false,
    val lyrics: String? = null
)

@Entity(
    tableName = "favorites",
    foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)]
)
data class FavoriteEntity(
    @PrimaryKey val trackId: String,
    val addedAt: Long
)

@Entity(
    tableName = "playback_history",
    foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("trackId"), Index("playedAt")]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val playedAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

/** Playlists only reference track ids; the audio file is never copied. Deleting either side removes the link. */
@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("trackId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: String,
    val position: Int
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, VERIFYING, EXTRACTING_METADATA, REGISTERING_LIBRARY,
    COMPLETED, FAILED, CANCELLED, PAUSED;

    val isActive: Boolean
        get() = this == PENDING || this == DOWNLOADING || this == VERIFYING ||
                this == EXTRACTING_METADATA || this == REGISTERING_LIBRARY
}

@Entity(
    tableName = "downloads",
    foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("trackId"), Index("status"), Index(value = ["sourceId", "format"], unique = true)]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val sourceUrl: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val durationSec: Long,
    /** AudioFormat enum name */
    val format: String,
    /** DownloadStatus enum name */
    val status: String,
    val fileSize: Long? = null,
    val error: String? = null,
    val trackId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    val statusEnum: DownloadStatus get() = DownloadStatus.valueOf(status)
}

/** A track joined with its artist, album and favorite flag: what every screen and the player work with. */
data class Track(
    @Embedded val entity: TrackEntity,
    val artistName: String,
    val albumTitle: String?,
    val albumArtist: String?,
    val isFavorite: Boolean
) {
    val id: String get() = entity.id
    val title: String get() = entity.title
    val album: String get() = albumTitle.orEmpty()
}

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int
)
