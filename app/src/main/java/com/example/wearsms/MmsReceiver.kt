package com.example.wearsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Required for the SMS role; MMS is intentionally unsupported. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
