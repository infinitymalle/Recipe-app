package dev.malkolm.recipeapp.ui.shoppinglist

import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.FakeShoppingListRepository
import dev.malkolm.recipeapp.testutil.FakeThemeSettingsRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class ShoppingListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `adding a manual item shows up in the list`() = runTest {
        val vm =
            ShoppingListViewModel(FakeShoppingListRepository(), FakeRecipeRepository(), FakeThemeSettingsRepository())

        vm.addItem("Eggs", "6")

        val state = vm.uiState.first { it.items.isNotEmpty() }
        assertEquals("Eggs", state.items.single().name)
        assertEquals("6", state.items.single().amount)
    }

    @Test
    fun `adding an item with a blank name does nothing`() = runTest {
        val vm =
            ShoppingListViewModel(FakeShoppingListRepository(), FakeRecipeRepository(), FakeThemeSettingsRepository())

        vm.addItem("   ", "6")

        assertTrue(vm.uiState.first().items.isEmpty())
    }

    @Test
    fun `checking and removing an item`() = runTest {
        val vm =
            ShoppingListViewModel(FakeShoppingListRepository(), FakeRecipeRepository(), FakeThemeSettingsRepository())
        vm.addItem("Eggs", "6")
        val id = vm.uiState.first { it.items.isNotEmpty() }.items.single().id

        vm.setChecked(id, true)
        assertTrue(vm.uiState.first().items.single().isChecked)

        vm.removeItem(id)
        assertTrue(vm.uiState.first().items.isEmpty())
    }

    @Test
    fun `clearing checked items only removes the checked ones`() = runTest {
        val vm =
            ShoppingListViewModel(FakeShoppingListRepository(), FakeRecipeRepository(), FakeThemeSettingsRepository())
        vm.addItem("Eggs", "6")
        vm.addItem("Flour", "2 cups")
        val eggsId = vm.uiState.first { it.items.size == 2 }.items.first { it.name == "Eggs" }.id
        vm.setChecked(eggsId, true)

        vm.clearChecked()

        val remaining = vm.uiState.first()
        assertEquals(listOf("Flour"), remaining.items.map { it.name })
    }

    @Test
    fun `adding from a recipe merges its ingredient entries, skipping section headings`() = runTest {
        val recipeRepository = FakeRecipeRepository()
        recipeRepository.saveRecipe(
            RecipeDraft(id = "r1", title = "Pancakes", ingredients = "Batter:\nFlour (2 cups)\nEggs (2)\nSalt")
        )
        val vm = ShoppingListViewModel(FakeShoppingListRepository(), recipeRepository, FakeThemeSettingsRepository())

        vm.addFromRecipe("r1")

        val names = vm.uiState.first { it.items.isNotEmpty() }.items.map { it.name }.sorted()
        assertEquals(listOf("Eggs", "Flour", "Salt"), names)
    }

    @Test
    fun `the recipe picker list mirrors the recipe repository`() = runTest {
        val recipeRepository = FakeRecipeRepository()
        recipeRepository.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        val vm = ShoppingListViewModel(FakeShoppingListRepository(), recipeRepository, FakeThemeSettingsRepository())

        val state = vm.uiState.first { it.recipes.isNotEmpty() }
        assertEquals(listOf("Pancakes"), state.recipes.map { it.title })
    }
}
