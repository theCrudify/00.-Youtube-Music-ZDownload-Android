package com.thecrudify.zdownload.data.metadata

import com.thecrudify.zdownload.ui.t
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
        if (!file.isFile || file.length() == 0L) throw UnreadableAudioException(t("File kosong atau tidak ditemukan", "File is empty or missing"))
        val mmr = MediaMetadataRetriever()
        try {
            try {
                mmr.setDataSource(file.absolutePath)
            } catch (e: RuntimeException) {
                throw UnreadableAudioException(t("File audio rusak atau formatnya tidak didukung", "Audio file is damaged or its format isn't supported"))
            }
            fun text(key: Int) = mmr.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }
            // "3/12" and "2019-05-01" style values: keep the leading number
            fun number(key: Int) = text(key)?.takeWhile { it.isDigit() }?.toIntOrNull()

            val duration = text(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val hasAudio = text(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
            if (duration <= 0L || !hasAudio) throw UnreadableAudioException(t("File audio rusak atau formatnya tidak didukung", "Audio file is damaged or its format isn't supported"))

            val lyrics = runCatching {
                val lrcFile = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
                if (lrcFile.isFile) {
                    lrcFile.readText().trim().takeIf { it.isNotEmpty() }
                } else if (file.extension.lowercase() in setOf("mp3", "flac", "m4a", "ogg")) {
                    val audio = org.jaudiotagger.audio.AudioFileIO.read(file)
                    audio.tag?.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)?.trim()?.takeIf { it.isNotEmpty() }
                } else null
            }.getOrNull()

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
                artwork = mmr.embeddedPicture,
                lyrics = lyrics
            )
        } finally {
            runCatching { mmr.release() }
        }
    }
}
