package dev.malkolm.recipeapp.data.examples

import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import dev.malkolm.recipeapp.ui.cookrecipe.stepsFrom
import dev.malkolm.recipeapp.ui.recipeedit.IngredientListItem
import dev.malkolm.recipeapp.ui.recipeedit.ingredientItemsFrom
import dev.malkolm.recipeapp.ui.recipeedit.toIngredientsText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ExampleRecipesTest {
    private val examples = ExampleRecipes.all(SequentialIdGenerator()::newId)

    @Test
    fun `every example is tagged Example and has ingredients, several steps and tips`() {
        for (recipe in examples) {
            assertTrue(recipe.tags.any { it.name == ExampleRecipes.TAG }, recipe.title)
            assertTrue(recipe.ingredients.isNotBlank(), recipe.title)
            assertTrue(stepsFrom(recipe.notes).size >= 3, recipe.title)
            assertTrue(recipe.attachments.isNotEmpty(), recipe.title)
        }
    }

    @Test
    fun `ingredients are written in the edit screen's format, so editing them changes nothing`() {
        for (recipe in examples) {
            val items = ingredientItemsFrom(recipe.ingredients) { "id" }
            assertEquals(recipe.ingredients, items.toIngredientsText(), recipe.title)
            assertTrue(items.first() is IngredientListItem.Heading, recipe.title)
        }
    }

    @Test
    fun `ids are unique and fixed, so adding the examples twice does not duplicate them`() = runTest {
        assertEquals(examples.size, examples.map { it.id }.toSet().size)
        val attachmentIds = examples.flatMap { recipe -> recipe.attachments.map { it.id } }
        assertEquals(attachmentIds.size, attachmentIds.toSet().size)

        val repository = FakeRecipeRepository()
        repeat(2) { ExampleRecipes.all(SequentialIdGenerator()::newId).forEach { repository.saveRecipe(it) } }

        assertEquals(examples.size, repository.getAllRecipes().size)
    }
}
