package com.example.wearsms.data

data class Contact(val name: String, val number: String)

private val NUMBER_PATTERN = Regex("""\+?\d{3,15}""")

/** Returns the number without formatting characters, or null if it is not a sendable phone number or short code. */
fun validNumber(input: String): String? =
    input.filterNot { it in " -.()" }.takeIf { NUMBER_PATTERN.matches(it) }

fun distinctContacts(contacts: List<Contact>): List<Contact> =
    contacts.distinctBy { it.name to it.number.filter(Char::isDigit).takeLast(10) }
