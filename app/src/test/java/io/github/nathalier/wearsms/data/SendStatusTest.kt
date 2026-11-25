package io.github.nathalier.wearsms.data

import android.app.Activity
import android.provider.Telephony.Sms
import org.junit.Assert.assertEquals
import org.junit.Test

class SendStatusTest {

    private val ok = Activity.RESULT_OK
    private val genericFailure = 1

    @Test
    fun `a successful part marks a pending message as sent`() {
        assertEquals(Sms.MESSAGE_TYPE_SENT, statusAfterSend(Sms.MESSAGE_TYPE_OUTBOX, ok))
    }

    @Test
    fun `a failed part marks a pending message as failed`() {
        assertEquals(Sms.MESSAGE_TYPE_FAILED, statusAfterSend(Sms.MESSAGE_TYPE_OUTBOX, genericFailure))
    }

    @Test
    fun `a later successful part does not hide an earlier failed part`() {
        assertEquals(Sms.MESSAGE_TYPE_FAILED, statusAfterSend(Sms.MESSAGE_TYPE_FAILED, ok))
    }

    @Test
    fun `a failed part overrides an already sent part`() {
        assertEquals(Sms.MESSAGE_TYPE_FAILED, statusAfterSend(Sms.MESSAGE_TYPE_SENT, genericFailure))
    }

    @Test
    fun `pending outbox messages are recognised as pending`() {
        assertEquals(MessageType.PENDING, MessageType.fromProvider(Sms.MESSAGE_TYPE_OUTBOX))
        assertEquals(MessageType.PENDING, MessageType.fromProvider(Sms.MESSAGE_TYPE_QUEUED))
    }
}
