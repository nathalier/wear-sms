package com.example.wearsms.data

import org.json.JSONArray
import org.json.JSONObject

fun exportJson(messages: List<Message>): String =
    JSONArray(
        messages.sortedBy { it.date }.map {
            JSONObject().put("address", it.address).put("date", it.date).put("type", it.type.name.lowercase()).put("body", it.body)
        },
    ).toString(2)

fun exportFileName(label: String, timestamp: Long): String =
    "wear-sms-${label.replace(Regex("[^A-Za-z0-9+_-]"), "_")}-$timestamp.json"
