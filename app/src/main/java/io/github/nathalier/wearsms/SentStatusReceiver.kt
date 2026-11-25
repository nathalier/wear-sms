package io.github.nathalier.wearsms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import io.github.nathalier.wearsms.data.statusAfterSend

class SentStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val resolver = context.contentResolver
        val currentType = resolver.query(uri, arrayOf(Sms.TYPE), null, null, null)?.use { if (it.moveToFirst()) it.getInt(0) else null } ?: return
        resolver.update(uri, ContentValues().apply { put(Sms.TYPE, statusAfterSend(currentType, resultCode)) }, null, null)
    }
}
