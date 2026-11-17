package com.thecrudify.zdownload.data.metadata

import android.media.MediaMetadataRetriever
import android.os.Build
import java.io.File

data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long,
    val bitrate: Int?,
    val sampleRate: Int?,
    val mimeType: String,
    val artwork: ByteArray?,
    val lyrics: String? = null
)

class UnreadableAudioException(message: String) : Exception(message)

/**
 * Reads tags and technical properties with the platform's MediaMetadataRetriever: it covers every
 * format the player itself can decode (MP3, FLAC, WAV, M4A/AAC, OGG, Opus) with no extra dependency.
 * It cannot write; see [TagWriter] for that.
 */
object MetadataReader {

    /** @throws UnreadableAudioException if the file is corrupt, empty or not audio the device can decode. */
    fun read(file: File): AudioMetadata {
        if (!file.isFile || file.length() == 0L) throw UnreadableAudioException("File kosong atau tidak ditemukan")
        val mmr = MediaMetadataRetriever()
        try {
            try {
                mmr.setDataSource(file.absolutePath)
            } catch (e: RuntimeException) {
                throw UnreadableAudioException("File audio rusak atau formatnya tidak didukung")
            }
            fun text(key: Int) = mmr.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }
            // "3/12" and "2019-05-01" style values: keep the leading number
            fun number(key: Int) = text(key)?.takeWhile { it.isDigit() }?.toIntOrNull()

            val duration = text(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val hasAudio = text(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
            if (duration <= 0L || !hasAudio) throw UnreadableAudioException("File audio rusak atau formatnya tidak didukung")

            return AudioMetadata(
                title = text(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = text(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = text(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                albumArtist = text(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                genre = text(MediaMetadataRetriever.METADATA_KEY_GENRE),
                year = (number(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: number(MediaMetadataRetriever.METADATA_KEY_DATE))?.takeIf { it in 1000..9999 },
                trackNumber = number(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
                discNumber = number(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER),
                durationMs = duration,
                bitrate = number(MediaMetadataRetriever.METADATA_KEY_BITRATE),
                sampleRate = if (Build.VERSION.SDK_INT >= 31) number(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE) else null,
                mimeType = text(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/*",
                artwork = mmr.embeddedPicture
            )
        } finally {
            runCatching { mmr.release() }
        }
    }
}
