package dev.malkolm.recipeapp.domain.model

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class TagNamesTest {
    @Test
    fun `clean trims and collapses whitespace`() {
        assertEquals("Quick dinner", TagNames.clean("  Quick \t  dinner\n"))
    }

    @Test
    fun `key ignores case for Swedish letters too`() {
        assertEquals(TagNames.key("Äpple"), TagNames.key("äpple"))
        assertEquals("åäö", TagNames.key("ÅÄÖ"))
    }

    @Test
    fun `different names have different keys`() {
        assertNotEquals(TagNames.key("Dessert"), TagNames.key("Desserts"))
    }

    @Test
    fun `clean makes composed and decomposed letters identical`() {
        val composed = "å" // å as one character
        val decomposed = "å" // a followed by a combining ring, as some keyboards produce
        assertEquals(composed, TagNames.clean(decomposed))
    }

    @Test
    fun `key does not depend on the phone's language setting`() {
        val original = Locale.getDefault()
        try {
            // In Turkish, a naive lowercase turns "I" into a dotless "ı". That would make tags
            // from a Turkish-language phone impossible to match.
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("i", TagNames.key("I"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `Tag of cleans the name and exposes a key`() {
        val tag = Tag.of(id = "t1", rawName = "  Vegetarian  ")

        assertEquals("Vegetarian", tag.name)
        assertEquals("vegetarian", tag.key)
    }

    @Test
    fun `Tag rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> { Tag.of(id = "t1", rawName = "   ") }
    }
}
