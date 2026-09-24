package dev.malkolm.recipeapp.ui.recipelist

import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import dev.malkolm.recipeapp.testutil.MutableClock
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class RecipeListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `shows empty when there are no recipes`() = runTest {
        val viewModel = RecipeListViewModel(FakeRecipeRepository())

        assertEquals(RecipeListUiState.Empty, viewModel.uiState.first { it !is RecipeListUiState.Loading })
    }

    @Test
    fun `shows saved recipes most recently updated first`() = runTest {
        val clock = MutableClock(Instant.parse("2026-01-01T10:00:00Z"))
        val repository = FakeRecipeRepository(clock)
        repository.saveRecipe(RecipeDraft(id = "1", title = "Pancakes"))
        clock.advanceBy(Duration.ofMinutes(1))
        repository.saveRecipe(RecipeDraft(id = "2", title = "Waffles"))
        val viewModel = RecipeListViewModel(repository)

        val content = viewModel.uiState.first { it !is RecipeListUiState.Loading } as RecipeListUiState.Content
        assertEquals(listOf("Waffles", "Pancakes"), content.recipes.map { it.title })
    }
}
