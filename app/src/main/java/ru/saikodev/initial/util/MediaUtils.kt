package ru.saikodev.initial.util

import android.content.Context
import android.webkit.MimeTypeMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object MediaUtils {
    const val MAX_MEDIA_BYTES = 100L * 1024 * 1024 // 100 MB
    const val MAX_DOC_BYTES = 50L * 1024 * 1024 // 50 MB
    private const val MEDIA_BASE_URL = "https://initial.su/api/get_media"

    fun getMediaUrl(key: String?, token: String?): String {
        if (key == null) return ""
        if (key.startsWith("http") || key.startsWith("data:")) return key
        val sb = StringBuilder(MEDIA_BASE_URL)
        sb.append("?key=").append(java.net.URLEncoder.encode(key, "UTF-8"))
        if (!token.isNullOrEmpty()) {
            sb.append("&token=").append(java.net.URLEncoder.encode(token, "UTF-8"))
        }
        return sb.toString()
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes Б"
            bytes < 1024 * 1024 -> String.format("%.1f КБ", bytes / 1024.0)
            else -> String.format("%.1f МБ", bytes / (1024.0 * 1024))
        }
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale("ru")).format(Date(timestamp * 1000))
    }

    fun formatDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val msg = Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
        return when {
            now.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR) -> "Сегодня"
            else -> {
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                if (yesterday.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
                    yesterday.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR)) "Вчера"
                else SimpleDateFormat("d MMM", Locale("ru")).format(Date(timestamp * 1000))
            }
        }
    }

    fun formatChatTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val ts = timestamp * 1000
        val now = Calendar.getInstance()
        val msg = Calendar.getInstance().apply { timeInMillis = ts }
        return when {
            now.get(Calendar.YEAR) == msg.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msg.get(Calendar.DAY_OF_YEAR) -> formatTime(timestamp)
            else -> SimpleDateFormat("d MMM", Locale("ru")).format(Date(ts))
        }
    }

    fun formatLastSeen(timestamp: Long?): String {
        if (timestamp == null) return "не в сети"
        val diff = (System.currentTimeMillis() / 1000) - timestamp
        return when {
            diff < 60 -> "только что"
            diff < 3600 -> "${diff / 60} мин. назад"
            else -> {
                val now = Calendar.getInstance()
                val last = Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (last.timeInMillis >= today.timeInMillis) {
                    "сегодня в ${formatTime(timestamp)}"
                } else {
                    "${SimpleDateFormat("d MMM", Locale("ru")).format(Date(timestamp * 1000))} в ${formatTime(timestamp)}"
                }
            }
        }
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    }

    fun isVideoFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("mp4", "webm", "mkv", "avi", "mov", "3gp")
    }

    fun isDocumentFile(file: File): Boolean {
        return !isImageFile(file) && !isVideoFile(file)
    }

    fun getDocumentIcon(ext: String): String {
        return when (ext.lowercase()) {
            "pdf" -> "📄"
            "doc", "docx" -> "📝"
            "xls", "xlsx" -> "📊"
            "ppt", "pptx" -> "📊"
            "txt", "md" -> "📃"
            "zip", "rar", "7z" -> "📦"
            "html", "css", "xml", "json" -> "💻"
            else -> "📎"
        }
    }

    fun initials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        return name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
            .take(2).joinToString("").uppercase()
    }

    fun getAvatarColor(name: String?): Long {
        val palette = listOf(
            0xFF8B5CF6, 0xFF3B82F6, 0xFF10B981, 0xFFF59E0B,
            0xFFEF4444, 0xFFEC4899, 0xFF14B8A6, 0xFFF97316,
            0xFF6366F1, 0xFF84CC16
        )
        var hash = 0
        for (c in (name ?: "").toCharArray()) {
            hash = c.code + ((hash shl 5) - hash)
        }
        return palette[kotlin.math.abs(hash) % palette.size]
    }

    fun hideSpoilerText(text: String): String {
        val brailleBlocks = "⡿⣟⣯⣷⣾⣽⣻⢿⣿⣶"
        return text.replace(Regex("\\|\\|([\\s\\S]*?)\\|\\|")) { match ->
            match.groupValues[1].mapIndexed { i, c ->
                if (c.isWhitespace()) c else brailleBlocks[(c.code + i) % brailleBlocks.length]
            }.joinToString("")
        }
    }
}
