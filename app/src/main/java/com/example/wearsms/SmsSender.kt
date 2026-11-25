package com.example.wearsms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import com.example.wearsms.data.Message

class SmsSender(private val context: Context) {

    private val resolver = context.contentResolver

    fun send(address: String, body: String) {
        val values = ContentValues().apply {
            put(Sms.ADDRESS, address)
            put(Sms.BODY, body)
            put(Sms.DATE, System.currentTimeMillis())
            put(Sms.READ, 1)
            put(Sms.STATUS, Sms.STATUS_PENDING)
        }
        val uri = resolver.insert(Sms.Outbox.CONTENT_URI, values) ?: return
        dispatch(uri, address, body)
    }

    fun retry(message: Message) {
        val uri = Uri.withAppendedPath(Sms.CONTENT_URI, message.id.toString())
        resolver.update(uri, ContentValues().apply { put(Sms.TYPE, Sms.MESSAGE_TYPE_OUTBOX); put(Sms.STATUS, Sms.STATUS_PENDING) }, null, null)
        dispatch(uri, message.address, message.body)
    }

    private fun dispatch(uri: Uri, address: String, body: String) {
        val manager = context.getSystemService(SmsManager::class.java)
        val parts = manager.divideMessage(body)
        val sentIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, SentStatusReceiver::class.java).setData(uri), PendingIntent.FLAG_IMMUTABLE,
        )
        val deliveryIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, DeliveryStatusReceiver::class.java).setData(uri), PendingIntent.FLAG_MUTABLE,
        )
        runCatching { manager.sendMultipartTextMessage(address, null, parts, ArrayList(parts.map { sentIntent }), ArrayList(parts.map { deliveryIntent })) }
            .onFailure { setType(uri, Sms.MESSAGE_TYPE_FAILED) }
    }

    private fun setType(uri: Uri, type: Int) {
        resolver.update(uri, ContentValues().apply { put(Sms.TYPE, type) }, null, null)
    }
}
