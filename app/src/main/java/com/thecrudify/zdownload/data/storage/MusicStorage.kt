package com.thecrudify.zdownload.data.storage

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.IOException

/** Characters Android/FAT filesystems reject, plus control characters. Titles come from the internet: never trust them. */
private val ILLEGAL = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

/** A filesystem-safe name for one path segment. */
fun safeFileName(raw: String, fallback: String = "Unknown"): String {
    val cleaned = raw.replace(ILLEGAL, " ").replace(Regex("\\s+"), " ").trim().trim('.')
    return cleaned.take(80).trim().ifEmpty { fallback }
}

/**
 * Owns every file the app writes:
 *  - audio:   Android/media/<package>/Music/<Artist>/<Title>.<ext>  (visible to other players, no permission needed)
 *  - artwork: <internal files>/artwork/<trackId>.<ext>
 *  - temp:    <cache>/download/…  (partial downloads; safe to clear when nothing is downloading)
 */
class MusicStorage(private val context: Context) {

    val musicDir: File
        get() {
            val base = context.externalMediaDirs.firstOrNull { it != null }
                ?: context.getExternalFilesDir(null)
                ?: context.filesDir
            return File(base, "Music").apply { mkdirs() }
        }

    val artworkDir: File get() = File(context.filesDir, "artwork").apply { mkdirs() }
    val tempDir: File get() = File(context.cacheDir, "download").apply { mkdirs() }

    fun freeBytes(): Long = runCatching { StatFs(musicDir.path).availableBytes }.getOrDefault(0L)
    fun totalBytes(): Long = runCatching { StatFs(musicDir.path).totalBytes }.getOrDefault(0L)

    /** A free path for a new track; appends " (2)", " (3)"… instead of overwriting a different song. */
    fun newTrackFile(artist: String, title: String, extension: String): File {
        val dir = File(musicDir, safeFileName(artist, "Unknown Artist")).apply { mkdirs() }
        val name = safeFileName(title, "Unknown Track")
        var candidate = File(dir, "$name.$extension")
        var n = 2
        while (candidate.exists()) candidate = File(dir, "$name ($n).$extension").also { n++ }
        return candidate
    }

    /**
     * Moves a finished temp file into place atomically: the copy lands next to the target as `.part`
     * and is renamed only once complete, so a crash never leaves a half-written song in the library.
     */
    fun commit(source: File, target: File) {
        target.parentFile?.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        try {
            source.copyTo(part, overwrite = true)
            if (part.length() != source.length()) throw IOException("Salinan file tidak lengkap")
            if (!part.renameTo(target)) throw IOException("Gagal menyimpan ${target.name}")
        } finally {
            part.delete()
        }
        source.delete()
    }

    fun saveArtwork(trackId: String, bytes: ByteArray, extension: String = "jpg"): String {
        deleteArtwork(trackId)
        // Timestamp in the name: a replaced cover gets a new path, so image caches cannot show the old one
        return File(artworkDir, "$trackId-${System.currentTimeMillis()}.$extension").apply { writeBytes(bytes) }.absolutePath
    }

    fun deleteArtwork(trackId: String) {
        artworkDir.listFiles { f -> f.name.startsWith(trackId) }?.forEach { it.delete() }
    }

    fun sizeOf(dir: File): Long = dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    /** Removes partial downloads except the work folders still in use. Returns bytes freed. */
    fun clearTemp(keep: Set<String> = emptySet()): Long {
        var freed = 0L
        tempDir.listFiles()?.filter { it.name !in keep }?.forEach {
            freed += sizeOf(it)
            it.deleteRecursively()
        }
        return freed
    }
}
