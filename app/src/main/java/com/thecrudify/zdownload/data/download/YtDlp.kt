package com.thecrudify.zdownload.data.download

import java.net.URI

/** Pure helpers around yt-dlp's text output, kept free of Android classes so they are unit-testable. */
object YtDlp {

    private val HOSTS = setOf("youtube.com", "youtu.be", "youtube-nocookie.com")

    /** Only http(s) links to YouTube / YouTube Music are accepted as a source. */
    fun isSupportedUrl(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return false
        val host = uri.host?.lowercase() ?: return false
        return HOSTS.any { host == it || host.endsWith(".$it") }
    }

    private val SPEED = Regex("""at\s+([\d.]+\s?[KMG]?i?B/s)""")

    /** "[download]  42.0% of 3.27MiB at 1.20MiB/s ETA 00:02" -> "1.20MiB/s" */
    fun parseSpeed(line: String): String? = SPEED.find(line)?.groupValues?.get(1)?.replace(" ", "")

    /** yt-dlp failures arrive as a wall of warnings; show the ERROR lines, or the tail if there are none. */
    fun summarizeError(message: String?): String {
        if (message.isNullOrBlank()) return "Terjadi kesalahan"
        val errors = message.lines().map { it.trim() }.filter { it.startsWith("ERROR") }
        val text = if (errors.isNotEmpty()) errors.joinToString("\n") else message.trim().lines().takeLast(3).joinToString("\n")
        return text.take(400)
    }
}
