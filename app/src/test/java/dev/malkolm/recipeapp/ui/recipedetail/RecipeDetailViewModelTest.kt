package dev.malkolm.recipeapp.ui.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

/** Uses Robolectric (via [AndroidJUnit4]): [SavedStateHandle.toRoute] needs a real `Bundle`. */
@RunWith(AndroidJUnit4::class)
class RecipeDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun handleFor(recipeId: String) = SavedStateHandle(mapOf("recipeId" to recipeId))

    @Test
    fun `shows not found for an unknown recipe`() = runTest {
        val viewModel = RecipeDetailViewModel(handleFor("missing"), FakeRecipeRepository())

        assertEquals(
            RecipeDetailUiState.NotFound,
            viewModel.uiState.first { it !is RecipeDetailUiState.Loading }
        )
    }

    @Test
    fun `shows the saved recipe`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        val viewModel = RecipeDetailViewModel(handleFor("r1"), repository)

        val content = viewModel.uiState.first { it !is RecipeDetailUiState.Loading } as RecipeDetailUiState.Content
        assertEquals("Pancakes", content.recipe.title)
    }

    @Test
    fun `deleteRecipe removes the recipe from view`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        val viewModel = RecipeDetailViewModel(handleFor("r1"), repository)

        viewModel.deleteRecipe()

        assertEquals(
            RecipeDetailUiState.NotFound,
            viewModel.uiState.first { it !is RecipeDetailUiState.Loading }
        )
    }
}
