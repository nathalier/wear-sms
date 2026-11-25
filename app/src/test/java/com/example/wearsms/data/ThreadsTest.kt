package com.example.wearsms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadsTest {

    private fun message(id: Long, threadId: Long, date: Long, address: String = "+44100", body: String = "m$id", type: MessageType = MessageType.INBOX, read: Boolean = true) =
        Message(id, threadId, address, body, date, type, read)

    @Test
    fun `no messages gives no threads`() {
        assertTrue(summarize(emptyList()).isEmpty())
    }

    @Test
    fun `messages with the same thread id collapse into one summary showing the latest message`() {
        val summaries = summarize(listOf(message(1, 7, date = 10), message(2, 7, date = 30), message(3, 7, date = 20)))

        assertEquals(1, summaries.size)
        assertEquals(ThreadSummary(threadId = 7, address = "+44100", snippet = "m2", date = 30, unreadCount = 0), summaries.single())
    }

    @Test
    fun `threads are ordered by latest message first`() {
        val summaries = summarize(listOf(message(1, 1, date = 10), message(2, 2, date = 50), message(3, 3, date = 30)))

        assertEquals(listOf(2L, 3L, 1L), summaries.map { it.threadId })
    }

    @Test
    fun `only unread inbox messages count as unread`() {
        val summaries = summarize(
            listOf(
                message(1, 1, date = 10, read = false),
                message(2, 1, date = 20, read = false),
                message(3, 1, date = 30, type = MessageType.SENT, read = false),
                message(4, 1, date = 40, read = true),
            ),
        )

        assertEquals(2, summaries.single().unreadCount)
    }

    @Test
    fun `summary address comes from the latest message`() {
        val summaries = summarize(listOf(message(1, 1, date = 10, address = "old"), message(2, 1, date = 20, address = "new")))

        assertEquals("new", summaries.single().address)
    }

    @Test
    fun `message type is mapped from the provider constant`() {
        assertEquals(MessageType.INBOX, MessageType.fromProvider(1))
        assertEquals(MessageType.SENT, MessageType.fromProvider(2))
        assertEquals(MessageType.FAILED, MessageType.fromProvider(5))
        assertEquals(MessageType.OTHER, MessageType.fromProvider(3))
    }
}
