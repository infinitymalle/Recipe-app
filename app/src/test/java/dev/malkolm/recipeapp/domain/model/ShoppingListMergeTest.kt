package dev.malkolm.recipeapp.domain.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShoppingListMergeTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun ids(vararg values: String): () -> String {
        var i = 0
        return { values[i++] }
    }

    @Test
    fun `combining amounts with the same unit sums them`() {
        assertEquals("3 cups", combineAmounts("2 cups", "1 cups"))
        assertEquals("3 cups", combineAmounts("2 cups", "1 CUPS"))
        assertEquals("5", combineAmounts("2", "3"))
        assertEquals("2.5 kg", combineAmounts("1 kg", "1.5 kg"))
    }

    @Test
    fun `combining amounts with different units keeps both`() {
        assertEquals("2 cups + 1 tbsp", combineAmounts("2 cups", "1 tbsp"))
    }

    @Test
    fun `combining with a missing amount keeps the other one`() {
        assertEquals("2 cups", combineAmounts(null, "2 cups"))
        assertEquals("2 cups", combineAmounts("2 cups", null))
        assertNull(combineAmounts(null, null))
        assertNull(combineAmounts("", "  "))
    }

    @Test
    fun `a brand new ingredient is added as a new unchecked item`() {
        val result =
            mergeShoppingEntries(
                existing = emptyList(),
                newEntries = listOf(ShoppingListEntry("Eggs", "6")),
                ids("a"),
                now
            )

        assertEquals(listOf(ShoppingListItem("a", "Eggs", "6", isChecked = false, addedAt = now)), result)
    }

    @Test
    fun `a matching name merges into the existing item instead of duplicating`() {
        val existing = listOf(ShoppingListItem("a", "Flour", "2 cups", isChecked = false, addedAt = now))

        val result = mergeShoppingEntries(existing, listOf(ShoppingListEntry("flour", "1 cups")), ids("b"), now)

        assertEquals(listOf(ShoppingListItem("a", "Flour", "3 cups", isChecked = false, addedAt = now)), result)
    }

    @Test
    fun `a matching name with no amount change produces no changes`() {
        val existing = listOf(ShoppingListItem("a", "Salt", null, isChecked = false, addedAt = now))

        val result = mergeShoppingEntries(existing, listOf(ShoppingListEntry("Salt", null)), ids(), now)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `a checked-off item is not matched, so the ingredient reappears as a fresh line`() {
        // Callers pass only unchecked items as "existing" (checked = already bought).
        val result =
            mergeShoppingEntries(
                existing = emptyList(),
                newEntries = listOf(ShoppingListEntry("Eggs", "6")),
                ids("new"),
                now
            )

        assertEquals("new", result.single().id)
        assertEquals(false, result.single().isChecked)
    }

    @Test
    fun `blank ingredient names are skipped`() {
        val result =
            mergeShoppingEntries(existing = emptyList(), newEntries = listOf(ShoppingListEntry("  ", "1")), ids(), now)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `several new entries each get their own id`() {
        val result =
            mergeShoppingEntries(
                existing = emptyList(),
                newEntries = listOf(ShoppingListEntry("Eggs", "6"), ShoppingListEntry("Milk", "1 L")),
                ids("a", "b"),
                now
            )

        assertEquals(listOf("Eggs", "Milk"), result.map { it.name })
        assertEquals(listOf("a", "b"), result.map { it.id })
    }
}
