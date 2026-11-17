package com.thecrudify.zdownload.data.model

enum class AudioFormat(
    val formatKey: String,
    val title: String,
    val badge: String,
    val description: String,
    val extension: String
) {
    ORIGINAL(
        formatKey = "best",
        title = "Original",
        badge = "Stream Asli",
        description = "Stream asli YouTube tanpa konversi ulang (Opus/M4A). Kualitas tertinggi.",
        extension = "opus"
    ),
    MP3(
        formatKey = "mp3",
        title = "MP3",
        badge = "320 kbps",
        description = "Kompatibel di semua perangkat. Dikonversi ulang ke 320kbps.",
        extension = "mp3"
    ),
    M4A(
        formatKey = "m4a",
        title = "M4A / AAC",
        badge = "256 kbps",
        description = "Format Apple AAC kualitas tinggi standar industri.",
        extension = "m4a"
    ),
    FLAC(
        formatKey = "flac",
        title = "FLAC",
        badge = "Lossless",
        description = "Format lossless tanpa kompresi tambahan. File lebih besar.",
        extension = "flac"
    ),
    WAV(
        formatKey = "wav",
        title = "WAV",
        badge = "Uncompressed",
        description = "Audio uncompressed studio quality.",
        extension = "wav"
    ),
    OPUS(
        formatKey = "opus",
        title = "OPUS",
        badge = "High Efficiency",
        description = "Format Opus modern dengan kompresi audio efisien.",
        extension = "opus"
    )
}
