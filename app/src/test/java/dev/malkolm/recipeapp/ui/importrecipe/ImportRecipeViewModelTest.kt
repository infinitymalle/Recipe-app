package dev.malkolm.recipeapp.ui.importrecipe

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.data.backup.RecipeBackupService
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

/** Uses Robolectric (via [AndroidJUnit4]): [SavedStateHandle.toRoute] needs a real `Bundle`. */
@RunWith(AndroidJUnit4::class)
class ImportRecipeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun handleFor(uri: String) = SavedStateHandle(mapOf("uri" to uri))

    @Test
    fun `importing a valid shared recipe file shows the imported recipe's id`() = runTest {
        val sourceRepo = FakeRecipeRepository()
        sourceRepo.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        val exportService =
            RecipeBackupService(ApplicationProvider.getApplicationContext(), sourceRepo, SequentialIdGenerator())
        val sharedUri = Uri.fromFile(exportService.exportForSharing("r1")!!)

        val destinationRepo = FakeRecipeRepository()
        val importService =
            RecipeBackupService(
                ApplicationProvider.getApplicationContext(),
                destinationRepo,
                SequentialIdGenerator("copy")
            )
        val viewModel = ImportRecipeViewModel(handleFor(sharedUri.toString()), importService)

        val state = viewModel.uiState.first { it !is ImportRecipeUiState.Loading }
        // Shared recipes are imported as a copy with a fresh id (the first one the generator hands out).
        assertEquals(ImportRecipeUiState.Imported("copy-0"), state)
        assertEquals("Pancakes", destinationRepo.observeRecipe("copy-0").first()?.title)
    }

    @Test
    fun `importing a file that is not a recipe export shows a failure`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val badFile = File(context.cacheDir, "not-a-recipe.zip").apply { writeText("nonsense") }
        val badUri = Uri.fromFile(badFile)
        val service = RecipeBackupService(context, FakeRecipeRepository(), SequentialIdGenerator())
        val viewModel = ImportRecipeViewModel(handleFor(badUri.toString()), service)

        val state = viewModel.uiState.first { it !is ImportRecipeUiState.Loading }
        assertEquals(ImportRecipeUiState.Failed, state)
    }
}
