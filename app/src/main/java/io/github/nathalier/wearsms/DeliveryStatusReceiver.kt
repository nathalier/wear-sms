package io.github.nathalier.wearsms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import io.github.nathalier.wearsms.data.statusAfterDelivery

class DeliveryStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val pdu = intent.getByteArrayExtra("pdu") ?: return
        val report = SmsMessage.createFromPdu(pdu, intent.getStringExtra("format"))?.status ?: return
        val resolver = context.contentResolver
        val currentStatus = resolver.query(uri, arrayOf(Sms.STATUS), null, null, null)?.use { if (it.moveToFirst()) it.getInt(0) else null } ?: return
        resolver.update(uri, ContentValues().apply { put(Sms.STATUS, statusAfterDelivery(currentStatus, report)) }, null, null)
    }
}
