package io.github.nathalier.wearsms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import io.github.nathalier.wearsms.data.Message

class MessageNotifier(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun show(threadId: Long, address: String, name: String?, unread: List<Message>) {
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH))
        val sender = Person.Builder().setName(name ?: address).build()
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
        unread.sortedBy { it.date }.forEach { style.addMessage(it.body, it.date, sender) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentText("New message").build())
            .setContentIntent(openThreadIntent(threadId, address))
            .addAction(replyAction(threadId, address))
            .build()
        manager.notify(threadId.toInt(), notification)
    }

    fun dismiss(threadId: Long) = manager.cancel(threadId.toInt())

    private fun openThreadIntent(threadId: Long, address: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_THREAD_ID, threadId)
            .putExtra(EXTRA_ADDRESS, address)
        return PendingIntent.getActivity(context, threadId.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun replyAction(threadId: Long, address: String): NotificationCompat.Action {
        val intent = Intent(context, ReplyReceiver::class.java).putExtra(EXTRA_THREAD_ID, threadId).putExtra(EXTRA_ADDRESS, address)
        val pending = PendingIntent.getBroadcast(context, threadId.toInt(), intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action.Builder(R.drawable.ic_notification, "Reply", pending)
            .addRemoteInput(RemoteInput.Builder(REPLY_KEY).setLabel("Reply").build())
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
    }

    companion object {
        const val REPLY_KEY = "reply"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
        private const val CHANNEL_ID = "messages"
    }
}
