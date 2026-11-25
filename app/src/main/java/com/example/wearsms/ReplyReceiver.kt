package com.example.wearsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.wearsms.MessageNotifier.Companion.EXTRA_ADDRESS
import com.example.wearsms.MessageNotifier.Companion.EXTRA_THREAD_ID
import com.example.wearsms.MessageNotifier.Companion.REPLY_KEY
import com.example.wearsms.data.SmsRepository

class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reply = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(REPLY_KEY)?.toString()?.takeIf { it.isNotBlank() } ?: return
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        SmsSender(context).send(address, reply)
        SmsRepository(context.contentResolver).markRead(threadId)
        MessageNotifier(context).dismiss(threadId)
    }
}
