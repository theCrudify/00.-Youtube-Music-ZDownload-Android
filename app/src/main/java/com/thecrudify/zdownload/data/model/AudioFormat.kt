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
    FLAC(
        formatKey = "flac",
        title = "FLAC",
        badge = "Lossless",
        description = "Format lossless tanpa kompresi tambahan. File lebih besar.",
        extension = "flac"
    )
}
