package com.example.wearsms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony

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
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
    }
}
