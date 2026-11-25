package com.example.wearsms.data

import android.app.Activity
import android.provider.Telephony.Sms

/** A message is failed if any of its parts failed; otherwise it is sent once a part succeeds. */
fun statusAfterSend(currentType: Int, resultCode: Int): Int = when {
    resultCode != Activity.RESULT_OK -> Sms.MESSAGE_TYPE_FAILED
    currentType == Sms.MESSAGE_TYPE_FAILED -> Sms.MESSAGE_TYPE_FAILED
    else -> Sms.MESSAGE_TYPE_SENT
}

enum class DeliveryStatus {
    NONE, PENDING, DELIVERED, FAILED;

    companion object {
        fun fromProvider(status: Int) = when (status) {
            Sms.STATUS_COMPLETE -> DELIVERED
            Sms.STATUS_PENDING -> PENDING
            Sms.STATUS_FAILED -> FAILED
            else -> NONE
        }
    }
}

/** Report statuses: 0..31 delivered, 32..63 temporary problem, 64+ permanent failure. */
fun statusAfterDelivery(currentStatus: Int, reportStatus: Int): Int = when {
    reportStatus >= 64 -> Sms.STATUS_FAILED
    currentStatus == Sms.STATUS_FAILED -> Sms.STATUS_FAILED
    reportStatus >= 32 -> Sms.STATUS_PENDING
    else -> Sms.STATUS_COMPLETE
}
