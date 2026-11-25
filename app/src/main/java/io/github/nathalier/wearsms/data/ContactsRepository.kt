package io.github.nathalier.wearsms.data

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.PhoneLookup

class ContactsRepository(private val resolver: ContentResolver) {

    fun search(text: String): List<Contact> = safely(emptyList()) {
        val pattern = "%${text.trim()}%"
        resolver.query(Phone.CONTENT_URI, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), "${Phone.DISPLAY_NAME} LIKE ? OR ${Phone.NUMBER} LIKE ?", arrayOf(pattern, pattern), "${Phone.DISPLAY_NAME} COLLATE NOCASE ASC")
            ?.use { distinctContacts(readAll(it)).take(MAX_RESULTS) }
            .orEmpty()
    }

    fun nameFor(number: String): String? = safely(null) {
        resolver.query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)), arrayOf(PhoneLookup.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun readAll(cursor: Cursor): List<Contact> = buildList {
        while (cursor.moveToNext()) add(Contact(cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty()))
    }

    private fun <T> safely(fallback: T, block: () -> T): T = try {
        block()
    } catch (_: SecurityException) {
        fallback
    }

    private companion object {
        const val MAX_RESULTS = 30
    }
}
