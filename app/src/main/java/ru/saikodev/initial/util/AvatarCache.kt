package ru.saikodev.initial.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.saikodev.initial.data.preferences.TokenManager
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarCache @Inject constructor(
    private val context: Context,
    private val tokenManager: TokenManager
) {

    private val cacheDir: File = File(context.cacheDir, "avatar_cache").also { it.mkdirs() }

    // Memory cache — 4MB for avatar bitmaps (64x64 ~ 16KB each → ~256 avatars)
    private val memoryCache: LruCache<String, Bitmap> = LruCache<String, Bitmap>((4 * 1024 * 1024).toInt())

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun getCachedAvatar(url: String): Bitmap? {
        val key = url.hashCode().toString()
        // Check memory first
        memoryCache.get(key)?.let { return it }
        // Check disk
        val diskFile = File(cacheDir, "$key.webp")
        if (diskFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(key, bitmap)
                    return bitmap
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun cacheAvatar(url: String, bitmap: Bitmap) {
        val key = url.hashCode().toString()
        // Memory
        memoryCache.put(key, bitmap)
        // Disk
        val diskFile = File(cacheDir, "$key.webp")
        try {
            FileOutputStream(diskFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                out.flush()
            }
        } catch (_: Exception) {}
    }

    fun fetchAndCacheAvatar(url: String, callback: (Bitmap?) -> Unit) {
        val fullUrl = resolveAvatarUrl(url)
        // Check existing cache first
        getCachedAvatar(fullUrl)?.let {
            callback(it)
            return
        }

        val request = Request.Builder().url(fullUrl).build()
        httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val bitmap = try {
                    if (!response.isSuccessful) {
                        callback(null)
                        return
                    }
                    val body = response.body ?: run {
                        callback(null)
                        return
                    }
                    val bytes = body.bytes()
                    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (original != null) {
                        val sized = resizeBitmap(original, AVATAR_SIZE_PX, AVATAR_SIZE_PX)
                        val circle = makeCircleBitmap(sized)
                        cacheAvatar(fullUrl, circle)
                        // Recycle intermediate bitmaps
                        if (sized != original) sized.recycle()
                        original.recycle()
                        circle
                    } else null
                } catch (_: Exception) {
                    null
                } finally {
                    response.close()
                }
                callback(bitmap)
            }
        })
    }

    fun createLetterAvatar(name: String): Bitmap {
        val initial = MediaUtils.initials(name)
        val color = MediaUtils.getAvatarColor(name)
        val size = AVATAR_SIZE_PX

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw circle background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color.toInt()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = if (initial.length > 1) size * 0.38f else size * 0.5f
        val textY = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, textY, textPaint)

        return bitmap
    }

    private fun resolveAvatarUrl(url: String): String {
        if (url.startsWith("http")) return url
        val token = tokenManager.getToken() ?: ""
        return MediaUtils.getMediaUrl(url, token)
    }

    companion object {
        const val AVATAR_SIZE_PX = 192 // 64dp * 3x for xxhdpi
    }
}

// Bitmap utilities

fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    if (source.width == targetWidth && source.height == targetHeight) return source
    return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
}

fun makeCircleBitmap(source: Bitmap): Bitmap {
    val size = minOf(source.width, source.height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = android.graphics.Rect(0, 0, size, size)
    val rectF = android.graphics.RectF(rect)

    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(source, rect, rectF, paint)

    return output
}
