package com.example.wearsms.data

import android.content.ContentResolver
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.Telephony.Sms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class SmsRepository(private val resolver: ContentResolver) {

    // FIXME loads every message to build the thread list; switch to the conversations provider if this gets slow
    fun threads(): Flow<List<ThreadSummary>> = changes().map { summarize(query()) }.flowOn(Dispatchers.IO)

    fun conversation(threadId: Long): Flow<List<Message>> =
        changes().map { query("${Sms.THREAD_ID} = ?", arrayOf(threadId.toString())).sortedBy { it.date } }.flowOn(Dispatchers.IO)

    fun find(id: Long): Message? = query("${Sms._ID} = ?", arrayOf(id.toString())).firstOrNull()

    fun unread(threadId: Long): List<Message> = query("${Sms.THREAD_ID} = ?", arrayOf(threadId.toString())).filter { it.isUnreadInbox }

    fun all(): List<Message> = query()

    fun deleteMessage(id: Long) {
        resolver.delete(Sms.CONTENT_URI, "${Sms._ID} = ?", arrayOf(id.toString()))
    }

    fun deleteThread(threadId: Long) {
        resolver.delete(Sms.CONTENT_URI, "${Sms.THREAD_ID} = ?", arrayOf(threadId.toString()))
    }

    fun markRead(threadId: Long) {
        val values = ContentValues().apply { put(Sms.READ, 1) }
        resolver.update(Sms.Inbox.CONTENT_URI, values, "${Sms.THREAD_ID} = ? AND ${Sms.READ} = 0", arrayOf(threadId.toString()))
    }

    private fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(Sms.CONTENT_URI, true, observer)
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun query(selection: String? = null, args: Array<String>? = null): List<Message> =
        try {
            resolver.query(Sms.CONTENT_URI, PROJECTION, selection, args, "${Sms.DATE} DESC")?.use(::readAll).orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }

    private fun readAll(cursor: Cursor): List<Message> = buildList {
        while (cursor.moveToNext()) add(
            Message(
                id = cursor.getLong(0),
                threadId = cursor.getLong(1),
                address = cursor.getString(2).orEmpty(),
                body = cursor.getString(3).orEmpty(),
                date = cursor.getLong(4),
                type = MessageType.fromProvider(cursor.getInt(5)),
                read = cursor.getInt(6) == 1,
                deliveryStatus = DeliveryStatus.fromProvider(cursor.getInt(7)),
            ),
        )
    }

    private companion object {
        val PROJECTION = arrayOf(Sms._ID, Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY, Sms.DATE, Sms.TYPE, Sms.READ, Sms.STATUS)
    }
}
