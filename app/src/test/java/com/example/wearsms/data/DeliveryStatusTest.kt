package com.example.wearsms.data

import android.provider.Telephony.Sms
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryStatusTest {

    @Test
    fun `provider status codes map to delivery states`() {
        assertEquals(DeliveryStatus.NONE, DeliveryStatus.fromProvider(Sms.STATUS_NONE))
        assertEquals(DeliveryStatus.DELIVERED, DeliveryStatus.fromProvider(Sms.STATUS_COMPLETE))
        assertEquals(DeliveryStatus.PENDING, DeliveryStatus.fromProvider(Sms.STATUS_PENDING))
        assertEquals(DeliveryStatus.FAILED, DeliveryStatus.fromProvider(Sms.STATUS_FAILED))
    }

    @Test
    fun `a delivered report completes a pending message`() {
        assertEquals(Sms.STATUS_COMPLETE, statusAfterDelivery(Sms.STATUS_PENDING, reportStatus = 0))
    }

    @Test
    fun `a temporary report keeps the message pending`() {
        assertEquals(Sms.STATUS_PENDING, statusAfterDelivery(Sms.STATUS_PENDING, reportStatus = 32))
    }

    @Test
    fun `a permanent failure report marks the message failed`() {
        assertEquals(Sms.STATUS_FAILED, statusAfterDelivery(Sms.STATUS_PENDING, reportStatus = 64))
    }

    @Test
    fun `a delivered part does not hide an earlier failed part`() {
        assertEquals(Sms.STATUS_FAILED, statusAfterDelivery(Sms.STATUS_FAILED, reportStatus = 0))
    }

    @Test
    fun `a failure report overrides an already delivered part`() {
        assertEquals(Sms.STATUS_FAILED, statusAfterDelivery(Sms.STATUS_COMPLETE, reportStatus = 66))
    }
}
