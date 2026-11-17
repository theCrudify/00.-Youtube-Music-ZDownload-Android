package com.thecrudify.zdownload.data.model

import com.thecrudify.zdownload.ui.t

enum class AudioFormat(
    val formatKey: String,
    val title: String,
    val extension: String
) {
    ORIGINAL("best", "Original", "opus"),
    MP3("mp3", "MP3", "mp3"),
    M4A("m4a", "M4A / AAC", "m4a"),
    FLAC("flac", "FLAC", "flac"),
    WAV("wav", "WAV", "wav"),
    OPUS("opus", "OPUS", "opus");

    // Getters, not constructor values, so they follow a language change made after startup.
    val badge: String
        get() = when (this) {
            ORIGINAL -> t("Stream Asli", "Original Stream")
            MP3 -> "320 kbps"
            M4A -> "256 kbps"
            FLAC -> "Lossless"
            WAV -> t("Tanpa kompresi", "Uncompressed")
            OPUS -> t("Efisien", "High Efficiency")
        }

    val description: String
        get() = when (this) {
            ORIGINAL -> t("Stream asli YouTube tanpa konversi ulang (Opus/M4A). Kualitas tertinggi.", "Untouched YouTube stream, no re-encoding (Opus/M4A). Best quality.")
            MP3 -> t("Kompatibel di semua perangkat. Dikonversi ulang ke 320kbps.", "Plays on every device. Re-encoded to 320 kbps.")
            M4A -> t("Format Apple AAC kualitas tinggi standar industri.", "Industry-standard high-quality Apple AAC.")
            FLAC -> t("Format lossless tanpa kompresi tambahan. File lebih besar.", "Lossless with no extra compression. Larger files.")
            WAV -> t("Audio tanpa kompresi, kualitas studio.", "Uncompressed, studio-quality audio.")
            OPUS -> t("Format Opus modern dengan kompresi audio efisien.", "Modern Opus with efficient compression.")
        }
}
