package io.github.nathalier.wearsms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactsTest {

    @Test
    fun `formatting characters are stripped from a valid number`() {
        assertEquals("+447700900123", validNumber("+44 7700 900-123"))
        assertEquals("02079460958", validNumber("(020) 7946.0958"))
    }

    @Test
    fun `a leading plus is kept and short codes are accepted`() {
        assertEquals("+15551234567", validNumber("+1 555 123 4567"))
        assertEquals("447700", validNumber("447700"))
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(validNumber(""))
        assertNull(validNumber("   "))
    }

    @Test
    fun `letters are rejected`() {
        assertNull(validNumber("07700abc123"))
    }

    @Test
    fun `numbers outside 3 to 15 digits are rejected`() {
        assertNull(validNumber("12"))
        assertNull(validNumber("1234567890123456"))
        assertNull(validNumber("+"))
    }

    @Test
    fun `a plus anywhere but the start is rejected`() {
        assertNull(validNumber("44+7700900123"))
        assertNull(validNumber("++447700900123"))
    }

    @Test
    fun `contacts with the same name and number are listed once`() {
        val contacts = listOf(
            Contact("Ann", "+44 7700 900123"),
            Contact("Ann", "+447700900123"),
            Contact("Ann", "07700 900001"),
            Contact("Bob", "+447700900123"),
        )

        assertEquals(
            listOf(Contact("Ann", "+44 7700 900123"), Contact("Ann", "07700 900001"), Contact("Bob", "+447700900123")),
            distinctContacts(contacts),
        )
    }
}
