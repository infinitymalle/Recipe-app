package dev.malkolm.recipeapp.ui.recipeedit

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.data.RecipeImageStorage
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

/** Uses Robolectric (via [AndroidJUnit4]): [SavedStateHandle.toRoute] needs a real `Bundle`. */
@RunWith(AndroidJUnit4::class)
class RecipeEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val imageStorage = RecipeImageStorage(ApplicationProvider.getApplicationContext())

    private fun newRecipeHandle() = SavedStateHandle(mapOf("recipeId" to null))

    private fun editHandle(recipeId: String) = SavedStateHandle(mapOf("recipeId" to recipeId))

    private fun viewModel(
        handle: SavedStateHandle,
        repository: FakeRecipeRepository = FakeRecipeRepository(),
        idGenerator: SequentialIdGenerator = SequentialIdGenerator()
    ) = RecipeEditViewModel(handle, repository, idGenerator, imageStorage)

    @Test
    fun `starts blank for a new recipe`() = runTest {
        val vm = viewModel(newRecipeHandle())

        val state = vm.uiState.first { it is RecipeEditUiState.Editing } as RecipeEditUiState.Editing
        assertTrue(state.isNew)
        assertEquals("", state.title)
        assertTrue(state.ingredients.isEmpty())
    }

    @Test
    fun `saving a blank title shows an error and does not save`() = runTest {
        val vm = viewModel(newRecipeHandle())
        vm.uiState.first { it is RecipeEditUiState.Editing }

        vm.save()

        val state = vm.uiState.first() as RecipeEditUiState.Editing
        assertTrue(state.titleError)
        assertFalse(state.saved)
    }

    @Test
    fun `saving a new recipe writes it to the repository with parsed tags`() = runTest {
        val repository = FakeRecipeRepository()
        val vm = viewModel(newRecipeHandle(), repository)
        vm.uiState.first { it is RecipeEditUiState.Editing }

        vm.updateTitle("Pancakes")
        vm.updateTagsText("Breakfast, quick")
        vm.save()

        val state = vm.uiState.first { (it as RecipeEditUiState.Editing).saved } as RecipeEditUiState.Editing
        val saved = repository.observeRecipe(state.recipeId).first()
        assertEquals("Pancakes", saved?.title)
        assertEquals(listOf("Breakfast", "quick"), saved?.tags?.map { it.name })
    }

    @Test
    fun `ingredients are added, saved as readable text lines and reloaded as entries`() = runTest {
        val repository = FakeRecipeRepository()
        val vm = viewModel(newRecipeHandle(), repository)
        vm.uiState.first { it is RecipeEditUiState.Editing }

        vm.updateTitle("Pancakes")
        vm.addIngredient(name = "Flour", amount = "2 cups")
        vm.addIngredient(name = "Salt", amount = "")
        var state = vm.uiState.first() as RecipeEditUiState.Editing
        assertEquals(listOf("Flour" to "2 cups", "Salt" to null), state.ingredients.map { it.name to it.amount })

        vm.save()
        state = vm.uiState.first { (it as RecipeEditUiState.Editing).saved } as RecipeEditUiState.Editing
        val saved = repository.observeRecipe(state.recipeId).first()
        assertEquals("Flour (2 cups)\nSalt", saved?.ingredients)

        val reloaded =
            RecipeEditViewModel(editHandle(state.recipeId), repository, SequentialIdGenerator(), imageStorage)
        val reloadedState = reloaded.uiState.first { it is RecipeEditUiState.Editing } as RecipeEditUiState.Editing
        assertEquals(
            listOf("Flour" to "2 cups", "Salt" to null),
            reloadedState.ingredients.map {
                it.name to it.amount
            }
        )
    }

    @Test
    fun `ingredients can be removed`() = runTest {
        val vm = viewModel(newRecipeHandle())
        vm.uiState.first { it is RecipeEditUiState.Editing }

        vm.addIngredient(name = "Flour", amount = "")
        val added = (vm.uiState.first() as RecipeEditUiState.Editing).ingredients.single()

        vm.removeIngredient(added.id)

        assertTrue((vm.uiState.first() as RecipeEditUiState.Editing).ingredients.isEmpty())
    }

    @Test
    fun `servings and cooking time are set and cleared`() = runTest {
        val vm = viewModel(newRecipeHandle())
        vm.uiState.first { it is RecipeEditUiState.Editing }

        vm.setServings("4")
        vm.setCookingTime("30")
        var state = vm.uiState.first() as RecipeEditUiState.Editing
        assertEquals(4, state.servings)
        assertEquals(30, state.cookingTimeMinutes)

        vm.clearServings()
        vm.clearCookingTime()
        state = vm.uiState.first() as RecipeEditUiState.Editing
        assertNull(state.servings)
        assertNull(state.cookingTimeMinutes)
    }

    @Test
    fun `invalid servings text is ignored`() = runTest {
        val vm = viewModel(newRecipeHandle())
        vm.uiState.first { it is RecipeEditUiState.Editing }

        vm.setServings("not a number")

        assertNull((vm.uiState.first() as RecipeEditUiState.Editing).servings)
    }

    @Test
    fun `editing an existing recipe pre-fills its fields`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(
            RecipeDraft(id = "r1", title = "Pancakes", servings = 4, notes = "Fluffy")
        )
        val vm = viewModel(editHandle("r1"), repository)

        val state = vm.uiState.first { it is RecipeEditUiState.Editing } as RecipeEditUiState.Editing
        assertFalse(state.isNew)
        assertEquals("Pancakes", state.title)
        assertEquals(4, state.servings)
        assertEquals("Fluffy", state.notes)
    }

    @Test
    fun `attachments can be added and removed`() = runTest {
        val vm = viewModel(newRecipeHandle())
        vm.uiState.first { it is RecipeEditUiState.Editing }

        vm.addLinkAttachment(title = "Source", url = "https://example.com")
        var state = vm.uiState.first() as RecipeEditUiState.Editing
        assertEquals(1, state.addedAttachments.size)
        val attachment = state.addedAttachments.single() as Attachment.Link
        assertEquals("https://example.com", attachment.url)

        vm.removeAttachment(attachment.id)
        state = vm.uiState.first() as RecipeEditUiState.Editing
        assertTrue(state.addedAttachments.isEmpty())
    }
}
