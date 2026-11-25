package io.github.nathalier.wearsms

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.github.nathalier.wearsms.data.ContactsRepository
import io.github.nathalier.wearsms.data.SmsRepository

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isEmpty()) return
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, parts.first().originatingAddress)
            put(Telephony.Sms.BODY, parts.joinToString("") { it.messageBody.orEmpty() })
            put(Telephony.Sms.DATE, parts.first().timestampMillis)
            put(Telephony.Sms.READ, 0)
        }
        val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values) ?: return
        val repository = SmsRepository(context.contentResolver)
        val message = repository.find(ContentUris.parseId(uri)) ?: return
        MessageNotifier(context).show(message.threadId, message.address, ContactsRepository(context.contentResolver).nameFor(message.address), repository.unread(message.threadId))
    }
}
