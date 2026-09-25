package dev.malkolm.recipeapp.ui.recipeedit

import kotlin.test.Test
import kotlin.test.assertEquals

class IngredientListItemTest {
    private fun entry(name: String, amount: String? = null) =
        IngredientListItem.Entry(id = "id", name = name, amount = amount)

    private fun heading(text: String) = IngredientListItem.Heading(id = "id", text = text)

    /** Encodes [items] as stored text and decodes it again, as saving then re-opening a recipe does. */
    private fun roundTrip(vararg items: IngredientListItem) =
        ingredientItemsFrom(items.toList().toIngredientsText()) { "id" }

    @Test
    fun `plain names, amounts and headings round-trip`() {
        val items = arrayOf(heading("Batter"), entry("Flour", "2 cups"), entry("Salt"))

        assertEquals(items.toList(), roundTrip(*items))
        assertEquals("Batter:\nFlour (2 cups)\nSalt", items.toList().toIngredientsText())
    }

    @Test
    fun `a name ending in brackets without an amount keeps its brackets`() {
        assertEquals(listOf(entry("Butter (softened)")), roundTrip(entry("Butter (softened)")))
        assertEquals("Butter (softened) ()", listOf(entry("Butter (softened)")).toIngredientsText())
    }

    @Test
    fun `a name with brackets and an amount`() {
        assertEquals(
            listOf(entry("Tomatoes (canned)", "400 g")),
            roundTrip(entry("Tomatoes (canned)", "400 g"))
        )
    }

    @Test
    fun `an amount may contain brackets`() {
        assertEquals(listOf(entry("Flour", "2 cups (250 g)")), roundTrip(entry("Flour", "2 cups (250 g)")))
    }

    @Test
    fun `a name ending in a colon stays an ingredient, not a heading`() {
        assertEquals(listOf(entry("Salt:")), roundTrip(entry("Salt:")))
    }

    @Test
    fun `text saved before brackets were supported still reads the same`() {
        assertEquals(
            listOf(heading("Sauce"), entry("Flour", "2 cups"), entry("Salt")),
            ingredientItemsFrom("Sauce:\nFlour (2 cups)\nSalt") { "id" }
        )
    }
}
