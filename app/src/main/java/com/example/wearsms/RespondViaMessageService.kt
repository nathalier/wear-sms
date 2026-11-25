package com.example.wearsms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Required for the SMS role; quick replies from the dialer are not handled yet. */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
