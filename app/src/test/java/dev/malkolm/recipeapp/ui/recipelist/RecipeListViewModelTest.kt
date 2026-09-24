package dev.malkolm.recipeapp.ui.recipelist

import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.FakeTagRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import dev.malkolm.recipeapp.testutil.MutableClock
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class RecipeListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun content(uiState: RecipeListUiState) = uiState as RecipeListUiState.Content

    @Test
    fun `shows empty when there are no recipes`() = runTest {
        val viewModel = RecipeListViewModel(FakeRecipeRepository(), FakeTagRepository())

        val state = content(viewModel.uiState.first { it !is RecipeListUiState.Loading })
        assertTrue(state.recipes.isEmpty())
    }

    @Test
    fun `shows saved recipes most recently updated first`() = runTest {
        val clock = MutableClock(Instant.parse("2026-01-01T10:00:00Z"))
        val repository = FakeRecipeRepository(clock)
        repository.saveRecipe(RecipeDraft(id = "1", title = "Pancakes"))
        clock.advanceBy(Duration.ofMinutes(1))
        repository.saveRecipe(RecipeDraft(id = "2", title = "Waffles"))
        val viewModel = RecipeListViewModel(repository, FakeTagRepository())

        val state = content(viewModel.uiState.first { it !is RecipeListUiState.Loading })
        assertEquals(listOf("Waffles", "Pancakes"), state.recipes.map { it.title })
    }

    @Test
    fun `search query filters recipes by title`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "1", title = "Pancakes"))
        repository.saveRecipe(RecipeDraft(id = "2", title = "Waffles"))
        val viewModel = RecipeListViewModel(repository, FakeTagRepository())
        viewModel.uiState.first { it !is RecipeListUiState.Loading }

        viewModel.updateSearchQuery("pan")

        val state = content(viewModel.uiState.first { content(it).searchQuery == "pan" })
        assertEquals(listOf("Pancakes"), state.recipes.map { it.title })
    }

    @Test
    fun `selecting a tag filters recipes and selecting it again clears the filter`() = runTest {
        val breakfast = Tag.of("t1", "Breakfast")
        val clock = MutableClock(Instant.parse("2026-01-01T10:00:00Z"))
        val repository = FakeRecipeRepository(clock)
        repository.saveRecipe(RecipeDraft(id = "1", title = "Pancakes", tags = listOf(breakfast)))
        clock.advanceBy(Duration.ofMinutes(1))
        repository.saveRecipe(RecipeDraft(id = "2", title = "Waffles"))
        val viewModel = RecipeListViewModel(repository, FakeTagRepository(listOf(breakfast)))
        viewModel.uiState.first { it !is RecipeListUiState.Loading }

        viewModel.selectTag("t1")
        var state = content(viewModel.uiState.first { content(it).selectedTagId == "t1" })
        assertEquals(listOf("Pancakes"), state.recipes.map { it.title })

        viewModel.selectTag("t1")
        state = content(viewModel.uiState.first { content(it).selectedTagId == null })
        assertEquals(listOf("Waffles", "Pancakes"), state.recipes.map { it.title })
        assertNull(state.selectedTagId)
    }
}
