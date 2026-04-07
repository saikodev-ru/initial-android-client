package ru.saikodev.initial.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.saikodev.initial.data.api.InitialApi
import ru.saikodev.initial.data.api.dto.SendMessageRequest
import ru.saikodev.initial.util.NotificationHelper

class ActionReplyReceiver : BroadcastReceiver() {

    // Since BroadcastReceiver with Hilt injection is complex, we use a manual approach:
    // We directly use the application context to access SharedPreferences for the token
    // and create an OkHttp-based API call for sending the message.

    companion object {
        const val EXTRA_REPLY_TEXT = "reply_text"
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_SENDER_SIGNAL_ID = "sender_signal_id"
        const val EXTRA_SENDER_NAME = "sender_name"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "ActionReplyReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getIntExtra(EXTRA_CHAT_ID, 0)
        val senderSignalId = intent.getStringExtra(EXTRA_SENDER_SIGNAL_ID) ?: return
        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(EXTRA_REPLY_TEXT)
            ?.toString()

        if (replyText.isNullOrBlank()) {
            Log.w(TAG, "Empty reply text, ignoring")
            return
        }

        Log.d(TAG, "Reply received: chatId=$chatId, signalId=$senderSignalId, text=$replyText")

        // Update notification immediately to show user's reply
        NotificationHelper.updateNotificationWithReply(
            context = context,
            notificationId = notificationId,
            chatId = chatId,
            senderSignalId = senderSignalId,
            senderName = senderName,
            replyText = replyText
        )

        // Send the message to the server in background
        CoroutineScope(Dispatchers.IO).launch {
            sendMessage(context, senderSignalId, replyText)
        }
    }

    private fun sendMessage(context: Context, toSignalId: String, body: String) {
        try {
            val prefs = context.getSharedPreferences("initial_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("auth_token", null)
            if (token == null) {
                Log.w(TAG, "No auth token found, cannot send reply")
                return
            }

            // Build a simple Retrofit instance for the reply
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
            val client = okhttp3.OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl("https://initial.su/api/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            val api = retrofit.create(InitialApi::class.java)
            val response = api.sendMessage(SendMessageRequest(to_signal_id = toSignalId, body = body))

            if (response.ok) {
                Log.d(TAG, "Reply sent successfully: messageId=${response.message_id}")
            } else {
                Log.w(TAG, "Failed to send reply: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply message", e)
        }
    }

    private fun String.toMediaType(): okhttp3.MediaType = okhttp3.MediaType.get(this)
}
