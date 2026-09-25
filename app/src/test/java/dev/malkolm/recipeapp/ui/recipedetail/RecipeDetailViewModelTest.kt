package dev.malkolm.recipeapp.ui.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.data.backup.RecipeBackupService
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    private fun newBackupService(repository: RecipeRepository) = RecipeBackupService(
        ApplicationProvider.getApplicationContext(),
        repository,
        SequentialIdGenerator()
    )

    @Test
    fun `shows not found for an unknown recipe`() = runTest {
        val repository = FakeRecipeRepository()
        val viewModel = RecipeDetailViewModel(handleFor("missing"), repository, newBackupService(repository))

        assertEquals(
            RecipeDetailUiState.NotFound,
            viewModel.uiState.first { it !is RecipeDetailUiState.Loading }
        )
    }

    @Test
    fun `shows the saved recipe`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        val viewModel = RecipeDetailViewModel(handleFor("r1"), repository, newBackupService(repository))

        val content = viewModel.uiState.first { it !is RecipeDetailUiState.Loading } as RecipeDetailUiState.Content
        assertEquals("Pancakes", content.recipe.title)
    }

    @Test
    fun `deleteRecipe removes the recipe from view`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        val viewModel = RecipeDetailViewModel(handleFor("r1"), repository, newBackupService(repository))

        viewModel.deleteRecipe()

        assertEquals(
            RecipeDetailUiState.NotFound,
            viewModel.uiState.first { it !is RecipeDetailUiState.Loading }
        )
    }

    @Test
    fun `shareRecipe produces a shareable file for an existing recipe`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        val viewModel = RecipeDetailViewModel(handleFor("r1"), repository, newBackupService(repository))

        assertNotNull(viewModel.shareRecipe())
    }

    @Test
    fun `shareRecipe returns null for a recipe that does not exist`() = runTest {
        val repository = FakeRecipeRepository()
        val viewModel = RecipeDetailViewModel(handleFor("missing"), repository, newBackupService(repository))

        assertEquals(null, viewModel.shareRecipe())
    }
}
