package ru.saikodev.initial.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import ru.saikodev.initial.MainActivity
import ru.saikodev.initial.R
import ru.saikodev.initial.service.ActionReplyReceiver

object NotificationHelper {

    const val CHANNEL_MESSAGES = "messages"
    const val CHANNEL_CALLS = "calls"
    const val CHANNEL_MESSAGES_NAME = "Сообщения"
    const val CHANNEL_CALLS_NAME = "Звонки"

    private const val GROUP_PREFIX = "initial_chat_"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messagesChannel = android.app.NotificationChannel(
                CHANNEL_MESSAGES,
                CHANNEL_MESSAGES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о новых сообщениях"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 200, 100)
                enableLights(true)
                setBypassDnd(false)
            }

            val callsChannel = android.app.NotificationChannel(
                CHANNEL_CALLS,
                CHANNEL_CALLS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о входящих звонках"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(callsChannel)
        }
    }

    fun showMessageNotification(
        context: Context,
        chatId: Int,
        senderSignalId: String?,
        senderName: String,
        senderAvatarUrl: String?,
        messageBody: String,
        mediaType: String?,
        senderAvatarBitmap: Bitmap?,
        notificationId: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val groupKey = GROUP_PREFIX + chatId

        // Build notification body text
        val displayBody = buildDisplayBody(messageBody, mediaType)

        // Create the Person for MessagingStyle
        val person = Person.Builder()
            .setName(senderName)
            .setKey(senderSignalId ?: senderName)
            .apply {
                if (senderAvatarBitmap != null) {
                    setIcon(androidx.core.graphics.drawable.IconCompat.createWithBitmap(senderAvatarBitmap))
                }
            }
            .build()

        // MessagingStyle
        val style = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Вы").setKey("me").build()
        )
        style.addMessage(
            NotificationCompat.MessagingStyle.Message(displayBody, System.currentTimeMillis(), person)
        )
        style.isGroupConversation = false
        style.conversationTitle = null

        // Click intent → open chat
        val chatIntent = createChatIntent(context, chatId, senderSignalId, senderName)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            chatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reply action
        val replyLabel = "Ответить"
        val remoteInput = RemoteInput.Builder(ActionReplyReceiver.EXTRA_REPLY_TEXT)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(context, ActionReplyReceiver::class.java).apply {
            putExtra(ActionReplyReceiver.EXTRA_CHAT_ID, chatId)
            putExtra(ActionReplyReceiver.EXTRA_SENDER_SIGNAL_ID, senderSignalId)
            putExtra(ActionReplyReceiver.EXTRA_SENDER_NAME, senderName)
            putExtra(ActionReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            replyLabel,
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // Large icon — cached avatar or fallback letter avatar
        val largeIcon = senderAvatarBitmap
            ?: AvatarCache(context, ru.saikodev.initial.data.preferences.TokenManager(context)).createLetterAvatar(senderName)

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(largeIcon)
            .setStyle(style)
            .setContentTitle(senderName)
            .setContentText(displayBody)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setGroup(groupKey)
            .setGroupSummary(false)
            .addAction(replyAction)
            .setNumber(1)
            .build()

        notificationManager.notify(notificationId, notification)

        // Update group summary
        showGroupSummary(context, notificationManager, groupKey, contentIntent)
    }

    fun updateNotificationWithReply(
        context: Context,
        notificationId: Int,
        chatId: Int,
        senderSignalId: String?,
        senderName: String,
        replyText: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val groupKey = GROUP_PREFIX + chatId

        val mePerson = Person.Builder().setName("Вы").setKey("me").build()

        val style = NotificationCompat.MessagingStyle(mePerson)
        style.addMessage(
            NotificationCompat.MessagingStyle.Message(
                "Вы: $replyText",
                System.currentTimeMillis(),
                mePerson
            )
        )
        style.isGroupConversation = false

        val chatIntent = createChatIntent(context, chatId, senderSignalId, senderName)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            chatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(style)
            .setContentTitle(senderName)
            .setContentText("Вы: $replyText")
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setGroupSummary(false)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showGroupSummary(
        context: Context,
        notificationManager: NotificationManager,
        groupKey: String,
        contentIntent: PendingIntent
    ) {
        val groupSummary = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Use a deterministic ID for the group summary based on groupKey
        notificationManager.notify(groupKey.hashCode(), groupSummary)
    }

    private fun createChatIntent(
        context: Context,
        chatId: Int,
        senderSignalId: String?,
        senderName: String
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_id", chatId)
            putExtra("open_signal_id", senderSignalId)
            putExtra("open_partner_name", senderName)
        }
    }

    private fun buildDisplayBody(body: String?, mediaType: String?): String {
        if (!body.isNullOrBlank()) return body

        return when (mediaType) {
            "image" -> "\uD83D\uDDBC Фото"
            "video" -> "\uD83C\uDFAC Видео"
            "voice" -> "\uD83C\uDFA4 Голосовое сообщение"
            "document", "file" -> "\uD83D\uDCC4 Документ"
            "sticker" -> "\uD83C\uDFAF Стикер"
            "gif" -> "GIF"
            "animation" -> "\uD83C\uDFAF Анимация"
            else -> "Сообщение"
        }
    }

    fun generateNotificationId(chatId: Int, timestamp: Long): Int {
        return (chatId * 31 + timestamp).toInt()
    }
}
