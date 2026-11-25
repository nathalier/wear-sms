package io.github.nathalier.wearsms.data

data class Message(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: MessageType,
    val read: Boolean,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.NONE,
)

data class ThreadSummary(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val date: Long,
    val unreadCount: Int,
    val name: String? = null,
) {
    val title get() = name ?: address
}

val Message.isUnreadInbox get() = type == MessageType.INBOX && !read

enum class MessageType {
    INBOX, SENT, PENDING, FAILED, OTHER;

    companion object {
        fun fromProvider(type: Int) = when (type) {
            1 -> INBOX
            2 -> SENT
            4, 6 -> PENDING
            5 -> FAILED
            else -> OTHER
        }
    }
}

fun summarize(messages: List<Message>): List<ThreadSummary> =
    messages.groupBy { it.threadId }
        .map { (threadId, thread) ->
            val latest = thread.maxBy { it.date }
            ThreadSummary(
                threadId = threadId,
                address = latest.address,
                snippet = latest.body,
                date = latest.date,
                unreadCount = thread.count { it.isUnreadInbox },
            )
        }
        .sortedByDescending { it.date }
