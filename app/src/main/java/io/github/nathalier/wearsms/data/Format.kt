package io.github.nathalier.wearsms.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatTimestamp(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
    now: Long = System.currentTimeMillis(),
): String {
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    val sameYear = time.year == Instant.ofEpochMilli(now).atZone(zone).year
    return DateTimeFormatter.ofPattern(if (sameYear) "d MMM HH:mm:ss" else "d MMM yyyy HH:mm:ss", locale).format(time)
}

data class StatusParts(val timestamp: String?, val status: String?)

fun statusParts(
    message: Message,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
    now: Long = System.currentTimeMillis(),
): StatusParts {
    val time = formatTimestamp(message.date, zone, locale, now)
    return when (message.type) {
        MessageType.FAILED -> StatusParts(null, "Not sent - tap to retry")
        MessageType.PENDING -> StatusParts(null, "Sending...")
        MessageType.SENT -> StatusParts(time, deliveryLabel(message.deliveryStatus))
        else -> StatusParts(time, null)
    }
}

fun statusLine(
    message: Message,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
    now: Long = System.currentTimeMillis(),
): String = statusParts(message, zone, locale, now).let { listOfNotNull(it.timestamp, it.status).joinToString(STATUS_SEPARATOR) }

private const val STATUS_SEPARATOR = " · "

private fun deliveryLabel(status: DeliveryStatus) = when (status) {
    DeliveryStatus.DELIVERED -> "Delivered"
    DeliveryStatus.FAILED -> "Delivery failed"
    else -> "Sent"
}
