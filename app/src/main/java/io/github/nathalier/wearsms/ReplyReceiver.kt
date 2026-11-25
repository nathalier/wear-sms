package io.github.nathalier.wearsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import io.github.nathalier.wearsms.MessageNotifier.Companion.EXTRA_ADDRESS
import io.github.nathalier.wearsms.MessageNotifier.Companion.EXTRA_THREAD_ID
import io.github.nathalier.wearsms.MessageNotifier.Companion.REPLY_KEY
import io.github.nathalier.wearsms.data.SmsRepository

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
