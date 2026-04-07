package ru.saikodev.initial.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.saikodev.initial.data.api.InitialApi
import ru.saikodev.initial.data.api.dto.RegisterFcmRequest
import ru.saikodev.initial.data.preferences.TokenManager
import ru.saikodev.initial.util.AvatarCache
import ru.saikodev.initial.util.NotificationHelper

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FcmEntryPoint {
    fun api(): InitialApi
    fun tokenManager(): TokenManager
    fun avatarCache(): AvatarCache
}

class InitialFirebaseMessagingService : FirebaseMessagingService() {

    private val entryPoint by lazy {
        dagger.hilt.EntryPointAccessors.fromApplication(
            applicationContext,
            FcmEntryPoint::class.java
        )
    }

    private val api: InitialApi get() = entryPoint.api()
    private val tokenManager: TokenManager get() = entryPoint.tokenManager()
    private val avatarCache: AvatarCache get() = entryPoint.avatarCache()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        registerTokenWithServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (message.data.isEmpty()) {
            Log.w(TAG, "Received empty data payload")
            return
        }

        val data = message.data
        Log.d(TAG, "Received push: chat_id=${data["chat_id"]}, sender=${data["sender_name"]}")

        val chatId = data["chat_id"]?.toIntOrNull() ?: return
        val senderSignalId = data["sender_signal_id"] ?: ""
        val senderName = data["sender_name"] ?: "Неизвестный"
        val senderAvatarUrl = data["sender_avatar"]
        val body = data["body"]
        val mediaType = data["media_type"]

        // Try to get cached avatar first, then download if needed
        val cachedAvatar = if (!senderAvatarUrl.isNullOrBlank()) {
            avatarCache.getCachedAvatar(senderAvatarUrl)
        } else null

        val notificationId = NotificationHelper.generateNotificationId(chatId, System.currentTimeMillis())

        if (cachedAvatar != null) {
            // Avatar already cached — show notification immediately
            showNotification(
                chatId = chatId,
                senderSignalId = senderSignalId,
                senderName = senderName,
                senderAvatarUrl = senderAvatarUrl,
                body = body,
                mediaType = mediaType,
                avatarBitmap = cachedAvatar,
                notificationId = notificationId
            )
        } else if (!senderAvatarUrl.isNullOrBlank()) {
            // Download avatar, then show notification
            avatarCache.fetchAndCacheAvatar(senderAvatarUrl) { bitmap ->
                val avatar = bitmap
                    ?: avatarCache.createLetterAvatar(senderName)
                showNotification(
                    chatId = chatId,
                    senderSignalId = senderSignalId,
                    senderName = senderName,
                    senderAvatarUrl = senderAvatarUrl,
                    body = body,
                    mediaType = mediaType,
                    avatarBitmap = avatar,
                    notificationId = notificationId
                )
            }
        } else {
            // No avatar URL — use letter avatar
            showNotification(
                chatId = chatId,
                senderSignalId = senderSignalId,
                senderName = senderName,
                senderAvatarUrl = null,
                body = body,
                mediaType = mediaType,
                avatarBitmap = avatarCache.createLetterAvatar(senderName),
                notificationId = notificationId
            )
        }
    }

    private fun showNotification(
        chatId: Int,
        senderSignalId: String,
        senderName: String,
        senderAvatarUrl: String?,
        body: String?,
        mediaType: String?,
        avatarBitmap: android.graphics.Bitmap,
        notificationId: Int
    ) {
        NotificationHelper.showMessageNotification(
            context = this,
            chatId = chatId,
            senderSignalId = senderSignalId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            messageBody = body ?: "",
            mediaType = mediaType,
            senderAvatarBitmap = avatarBitmap,
            notificationId = notificationId
        )
    }

    private fun registerTokenWithServer(token: String) {
        if (tokenManager.getToken() == null) {
            Log.w(TAG, "User not logged in, skipping FCM registration")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.registerFcm(RegisterFcmRequest(fcm_token = token))
                if (response.ok) {
                    Log.d(TAG, "FCM token registered with server successfully")
                } else {
                    Log.w(TAG, "FCM token registration failed: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token with server", e)
            }
        }
    }

    companion object {
        private const val TAG = "InitialFCM"
    }
}
