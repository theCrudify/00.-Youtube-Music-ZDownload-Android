package com.thecrudify.zdownload.data.metadata

import com.thecrudify.zdownload.ui.t
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.IOException
import kotlin.math.abs

data class TagEdit(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int?,
    val trackNumber: Int?,
    val lyrics: String? = null
)

/**
 * Writes edited tags into the audio file itself (jaudiotagger). Opus/OGG/WAV are not written:
 * jaudiotagger has no Opus support, so for those the correction lives in the app database only.
 */
object TagWriter {

    private val WRITABLE = setOf("mp3", "flac", "m4a")

    fun canWrite(format: String): Boolean = format.lowercase() in WRITABLE

    /**
     * Tags a copy, checks the copy still decodes to the same duration, then swaps it in atomically.
     * The original is untouched if anything fails.
     */
    fun write(file: File, edit: TagEdit, artwork: File?) {
        if (!canWrite(file.extension)) throw IOException(t("Format .${file.extension} tidak bisa ditulis tag-nya", "Tags can't be written to .${file.extension} files"))
        val originalDuration = MetadataReader.read(file).durationMs
        // jaudiotagger picks its parser from the extension, so the working copy keeps it
        val work = File(file.parentFile, ".${file.nameWithoutExtension}.tagging.${file.extension}")
        try {
            file.copyTo(work, overwrite = true)
            val audio = AudioFileIO.read(work)
            val tag = audio.tagOrCreateAndSetDefault
            fun set(key: FieldKey, value: String?) {
                if (value.isNullOrBlank()) tag.deleteField(key) else tag.setField(key, value)
            }
            set(FieldKey.TITLE, edit.title)
            set(FieldKey.ARTIST, edit.artist)
            set(FieldKey.ALBUM, edit.album)
            set(FieldKey.ALBUM_ARTIST, edit.albumArtist)
            set(FieldKey.GENRE, edit.genre)
            set(FieldKey.YEAR, edit.year?.toString())
            set(FieldKey.TRACK, edit.trackNumber?.toString())
            set(FieldKey.LYRICS, edit.lyrics)
            if (artwork != null && artwork.isFile) {
                tag.deleteArtworkField()
                tag.setField(ArtworkFactory.createArtworkFromFile(artwork))
            }
            audio.commit()

            val written = MetadataReader.read(work)
            if (abs(written.durationMs - originalDuration) > 1500) {
                throw IOException(t("Hasil penulisan tag tidak lolos verifikasi; file asli tidak diubah", "Tag write failed verification; the original file was not changed"))
            }
            if (!work.renameTo(file)) throw IOException(t("Gagal mengganti file asli", "Couldn't replace the original file"))
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(t("Gagal menulis tag: ", "Couldn't write tags: ") + e.message, e)
        } finally {
            work.delete()
        }
    }
}
