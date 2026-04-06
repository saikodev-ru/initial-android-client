package ru.saikodev.initial.util

import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

object MediaUtils {

    private const val BASE_URL = "https://initial.su/api/get_media"

    fun getMediaUrl(key: String, token: String): String {
        if (key.startsWith("http") || key.startsWith("data:")) return key
        return "$BASE_URL?key=${URLEncoder.encode(key, "UTF-8")}&token=$token"
    }

    fun resolveUrl(url: String?): String? {
        if (url == null) return null
        if (url.startsWith("http") || url.startsWith("data:") || url.startsWith("blob:")) return url
        // Return relative URL — resolved with token at display time
        return url
    }

    fun getMediaUrlFull(url: String?, token: String): String? {
        if (url == null) return null
        return resolveUrl(url) ?: getMediaUrl(url, token)
    }

    fun getMimeType(file: File): String = when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "mp3" -> "audio/mpeg"
        "ogg" -> "audio/ogg"
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "zip" -> "application/zip"
        "rar" -> "application/vnd.rar"
        else -> "application/octet-stream"
    }

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes Б"
        bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} МБ"
        else -> "${bytes / (1024 * 1024 * 1024)} ГБ"
    }

    fun formatTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        val now = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }

        return when {
            isSameDay(now, date) -> "Сегодня"
            isYesterday(now, date) -> "Вчера"
            else -> SimpleDateFormat("d MMM", Locale("ru")).format(Date(timestamp * 1000))
        }
    }

    fun formatChatTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val now = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
        return if (isSameDay(now, date)) formatTime(timestamp) else formatDate(timestamp)
    }

    fun formatLastSeen(timestamp: Long?): String {
        if (timestamp == null) return "не в сети"
        val now = System.currentTimeMillis() / 1000
        val diff = now - timestamp
        return when {
            diff < 60 -> "только что"
            diff < 3600 -> "${diff / 60} мин. назад"
            diff < 86400 -> "сегодня"
            else -> {
                val sdf = SimpleDateFormat("d MMM в HH:mm", Locale("ru"))
                sdf.format(Date(timestamp * 1000))
            }
        }
    }

    fun isOnline(lastSeen: Long?): Boolean {
        if (lastSeen == null) return false
        return System.currentTimeMillis() / 1000 - lastSeen < 90
    }

    fun initials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        val parts = name.trim().split("\\s+".toRegex())
        return when (parts.size) {
            1 -> parts[0].take(1).uppercase()
            else -> (parts[0].take(1) + parts.last().take(1)).uppercase()
        }
    }

    fun getAvatarColor(name: String?): Long {
        if (name.isNullOrBlank()) return 0xFF5B5FC7.toInt().toLong()
        var hash = 0L
        for (c in name) hash = hash * 31 + c.code
        val colors = longArrayOf(
            0xFFE17076, 0xFF7BC862, 0xFF6EC9CB, 0xFF65AADD,
            0xFFEE7AAE, 0xFFFAA774, 0xFFA695E7, 0xFF6FB1FC
        )
        return colors[Math.abs(hash % colors.size).toInt()]
    }

    fun hideSpoilerText(text: String?): String {
        if (text == null) return ""
        return text.replace(Regex("\\|\\|(.+?)\\|\\|")) { "▓▓▓▓▓▓" }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(now: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, date)
    }
}
