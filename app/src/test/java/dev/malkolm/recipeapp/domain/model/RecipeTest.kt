package dev.malkolm.recipeapp.domain.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecipeTest {
    @Test
    fun `draft accepts unknown numbers`() {
        RecipeDraft(id = "r1", title = "Soup", servings = null, cookingTimeMinutes = null, rating = null)
    }

    @Test
    fun `draft accepts the boundary values`() {
        RecipeDraft(id = "r1", title = "Soup", servings = 1, cookingTimeMinutes = 0, rating = RecipeRules.MIN_RATING)
        RecipeDraft(id = "r1", title = "Soup", rating = RecipeRules.MAX_RATING)
    }

    @Test
    fun `draft rejects ratings outside 1 to 5`() {
        assertFailsWith<IllegalArgumentException> { RecipeDraft(id = "r1", title = "Soup", rating = 0) }
        assertFailsWith<IllegalArgumentException> { RecipeDraft(id = "r1", title = "Soup", rating = 6) }
    }

    @Test
    fun `draft rejects zero servings and negative cooking time`() {
        assertFailsWith<IllegalArgumentException> { RecipeDraft(id = "r1", title = "Soup", servings = 0) }
        assertFailsWith<IllegalArgumentException> { RecipeDraft(id = "r1", title = "Soup", cookingTimeMinutes = -5) }
    }

    @Test
    fun `recipe applies the same rules`() {
        assertFailsWith<IllegalArgumentException> {
            Recipe(
                id = "r1",
                title = "Soup",
                ingredients = "",
                servings = 2,
                cookingTimeMinutes = 10,
                rating = 9,
                notes = "",
                tags = emptyList(),
                attachments = emptyList(),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )
        }
    }

    @Test
    fun `toDraft keeps every editable field and drops the timestamps`() {
        val recipe =
            Recipe(
                id = "r1",
                title = "Soup",
                ingredients = "water",
                servings = 2,
                cookingTimeMinutes = 10,
                rating = 4,
                notes = "salty",
                tags = listOf(Tag("t1", "Quick")),
                attachments = listOf(Attachment.Text("a1", "stir")),
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )

        val expected =
            RecipeDraft(
                id = "r1",
                title = "Soup",
                ingredients = "water",
                servings = 2,
                cookingTimeMinutes = 10,
                rating = 4,
                notes = "salty",
                tags = listOf(Tag("t1", "Quick")),
                attachments = listOf(Attachment.Text("a1", "stir"))
            )
        assertEquals(expected, recipe.toDraft())
    }
}
