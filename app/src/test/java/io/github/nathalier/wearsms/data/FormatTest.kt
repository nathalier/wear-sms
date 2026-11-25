package io.github.nathalier.wearsms.data

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    private val utc = ZoneId.of("UTC")
    private val millisAt = { iso: String -> Instant.parse(iso).toEpochMilli() }
    private val now = millisAt("2026-11-25T10:00:00Z")

    private fun format(iso: String, zone: ZoneId = utc, now: Long = this.now) = formatTimestamp(millisAt(iso), zone, Locale.ENGLISH, now)

    @Test
    fun `timestamp in the current year omits the year`() {
        assertEquals("12 Aug 11:55:10", format("2026-08-12T11:55:10Z"))
    }

    @Test
    fun `timestamp in another year includes the year`() {
        assertEquals("3 Oct 2025 18:05:09", format("2025-10-03T18:05:09Z"))
    }

    @Test
    fun `single digit days are not padded and hours use 24 hour time`() {
        assertEquals("3 Oct 18:05:09", format("2026-10-03T18:05:09Z"))
    }

    @Test
    fun `timestamp is shown in the requested time zone`() {
        assertEquals("12 Aug 12:55:10", format("2026-08-12T11:55:10Z", ZoneId.of("Europe/London")))
    }

    @Test
    fun `the year is compared in the requested time zone`() {
        val auckland = ZoneId.of("Pacific/Auckland")
        val newYearsEveUtc = millisAt("2026-12-31T23:30:00Z")
        val afterMidnightUtc = millisAt("2027-01-01T00:30:00Z")

        assertEquals("1 Jan 12:30:00", formatTimestamp(newYearsEveUtc, auckland, Locale.ENGLISH, afterMidnightUtc))
        assertEquals("31 Dec 2026 23:30:00", formatTimestamp(newYearsEveUtc, utc, Locale.ENGLISH, afterMidnightUtc))
    }

    private fun message(type: MessageType, delivery: DeliveryStatus = DeliveryStatus.NONE) =
        Message(1, 1, "+44100", "hi", millisAt("2026-08-12T11:55:10Z"), type, read = true, deliveryStatus = delivery)

    private fun line(message: Message) = statusLine(message, utc, Locale.ENGLISH, now)

    @Test
    fun `received messages show only the timestamp`() {
        assertEquals("12 Aug 11:55:10", line(message(MessageType.INBOX)))
    }

    @Test
    fun `sent messages show the timestamp and whether a delivery report arrived`() {
        assertEquals("12 Aug 11:55:10 · Sent", line(message(MessageType.SENT)))
        assertEquals("12 Aug 11:55:10 · Sent", line(message(MessageType.SENT, DeliveryStatus.PENDING)))
        assertEquals("12 Aug 11:55:10 · Delivered", line(message(MessageType.SENT, DeliveryStatus.DELIVERED)))
        assertEquals("12 Aug 11:55:10 · Delivery failed", line(message(MessageType.SENT, DeliveryStatus.FAILED)))
    }

    @Test
    fun `pending and failed messages keep their own labels`() {
        assertEquals("Sending...", line(message(MessageType.PENDING)))
        assertEquals("Not sent - tap to retry", line(message(MessageType.FAILED)))
    }

    private fun parts(message: Message) = statusParts(message, utc, Locale.ENGLISH, now)

    @Test
    fun `received messages have a timestamp and no status`() {
        assertEquals(StatusParts("12 Aug 11:55:10", null), parts(message(MessageType.INBOX)))
    }

    @Test
    fun `sent messages have a timestamp and a delivery status`() {
        assertEquals(StatusParts("12 Aug 11:55:10", "Delivered"), parts(message(MessageType.SENT, DeliveryStatus.DELIVERED)))
    }

    @Test
    fun `pending and failed messages have a status and no timestamp`() {
        assertEquals(StatusParts(null, "Sending..."), parts(message(MessageType.PENDING)))
        assertEquals(StatusParts(null, "Not sent - tap to retry"), parts(message(MessageType.FAILED)))
    }
}
