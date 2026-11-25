package io.github.nathalier.wearsms.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportTest {

    private fun message(id: Long, date: Long, body: String = "m$id", type: MessageType = MessageType.INBOX, address: String = "+44100") =
        Message(id, 1, address, body, date, type, read = true)

    private fun parse(json: String) = JSONArray(json)

    @Test
    fun `no messages export as an empty array`() {
        assertEquals(0, parse(exportJson(emptyList())).length())
    }

    @Test
    fun `each message exports address date type and body`() {
        val exported = parse(exportJson(listOf(message(1, 1000, body = "hi", type = MessageType.SENT, address = "+44200")))).getJSONObject(0)

        assertEquals("+44200", exported.getString("address"))
        assertEquals(1000L, exported.getLong("date"))
        assertEquals("sent", exported.getString("type"))
        assertEquals("hi", exported.getString("body"))
    }

    @Test
    fun `messages are exported oldest first`() {
        val exported = parse(exportJson(listOf(message(1, 300), message(2, 100), message(3, 200))))

        assertEquals(listOf("m2", "m3", "m1"), List(3) { exported.getJSONObject(it).getString("body") })
    }

    @Test
    fun `quotes newlines and emoji survive a round trip`() {
        val body = "say \"hi\"\nline two 😀 \\ done"

        assertEquals(body, parse(exportJson(listOf(message(1, 1, body = body)))).getJSONObject(0).getString("body"))
    }

    @Test
    fun `message types are exported as lowercase names`() {
        val exported = parse(exportJson(MessageType.entries.mapIndexed { i, type -> message(i.toLong(), i.toLong(), type = type) }))

        assertEquals(listOf("inbox", "sent", "pending", "failed", "other"), List(5) { exported.getJSONObject(it).getString("type") })
    }

    @Test
    fun `file names keep only safe characters from the label`() {
        assertEquals("wear-sms-+447700900123-123.json", exportFileName("+447700900123", 123))
        assertEquals("wear-sms-Ann_Lee__-5.json", exportFileName("Ann Lee/\\", 5))
        assertEquals("wear-sms-all-9.json", exportFileName("all", 9))
    }
}
