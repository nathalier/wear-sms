package com.example.wearsms

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.example.wearsms.data.Message
import com.example.wearsms.data.exportFileName
import com.example.wearsms.data.exportJson

class MessageExporter(private val context: Context) {

    /** Writes the messages to Download/wear-sms and returns the path shown to the user. */
    fun export(label: String, messages: List<Message>): String {
        val name = exportFileName(label, System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
        }
        val uri = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "Could not create export file" }
        checkNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(exportJson(messages).toByteArray()) }
        return "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/$name"
    }

    private companion object {
        const val FOLDER = "wear-sms"
    }
}
