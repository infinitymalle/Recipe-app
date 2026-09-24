package dev.malkolm.recipeapp.ui.cookrecipe

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import dev.malkolm.recipeapp.timer.CookTimerRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

/** Uses Robolectric (via [AndroidJUnit4]): [SavedStateHandle.toRoute] needs a real `Bundle`. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CookRecipeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun handleFor(recipeId: String) = SavedStateHandle(mapOf("recipeId" to recipeId))

    private fun newCookTimerRepository() =
        CookTimerRepository(ApplicationProvider.getApplicationContext(), SequentialIdGenerator())

    @Test
    fun `steps come from notes split on blank lines`() {
        assertEquals(emptyList(), stepsFrom(""))
        assertEquals(emptyList(), stepsFrom("   \n\n "))
        assertEquals(listOf("Mix everything."), stepsFrom("Mix everything."))
        assertEquals(
            listOf("Mix flour and water.", "Knead for 10 minutes.", "Let it rest."),
            stepsFrom("Mix flour and water.\n\nKnead for 10 minutes.\n\nLet it rest.")
        )
    }

    @Test
    fun `loads the recipe's title, steps and suggested timer`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(
            RecipeDraft(id = "r1", title = "Pancakes", notes = "Mix.\n\nCook.", cookingTimeMinutes = 12)
        )
        val vm = CookRecipeViewModel(handleFor("r1"), repository, newCookTimerRepository())

        val state = vm.uiState.first { it is CookRecipeUiState.Content } as CookRecipeUiState.Content
        assertEquals("Pancakes", state.title)
        assertEquals(listOf("Mix.", "Cook."), state.steps)
        assertEquals(12, state.suggestedTimerMinutes)
        assertEquals(0, state.currentStepIndex)
    }

    @Test
    fun `shows not found for an unknown recipe`() = runTest {
        val vm = CookRecipeViewModel(handleFor("missing"), FakeRecipeRepository(), newCookTimerRepository())

        assertEquals(CookRecipeUiState.NotFound, vm.uiState.first { it !is CookRecipeUiState.Loading })
    }

    @Test
    fun `step navigation is clamped to the step range`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes", notes = "A.\n\nB."))
        val vm = CookRecipeViewModel(handleFor("r1"), repository, newCookTimerRepository())
        vm.uiState.first { it is CookRecipeUiState.Content }

        vm.previousStep()
        assertEquals(0, (vm.uiState.first() as CookRecipeUiState.Content).currentStepIndex)

        vm.nextStep()
        assertEquals(1, (vm.uiState.first() as CookRecipeUiState.Content).currentStepIndex)

        vm.nextStep()
        assertEquals(1, (vm.uiState.first() as CookRecipeUiState.Content).currentStepIndex)
    }

    @Test
    fun `a timer can be added, adjusted and removed`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Rice"))
        val vm = CookRecipeViewModel(handleFor("r1"), repository, newCookTimerRepository())
        vm.uiState.first { it is CookRecipeUiState.Content }

        vm.addTimer(minutes = 20, label = "Brown rice")
        var state = vm.uiState.first() as CookRecipeUiState.Content
        val timer = state.timers.single()
        assertEquals("Brown rice", timer.label)
        assertEquals(1200, timer.totalSeconds)
        assertEquals(1200, timer.remainingSeconds)
        assertTrue(timer.isRunning)

        vm.adjustTimer(timer.id, deltaMinutes = 5)
        state = vm.uiState.first() as CookRecipeUiState.Content
        assertEquals(1500, state.timers.single().totalSeconds)
        assertEquals(1500, state.timers.single().remainingSeconds)

        vm.removeTimer(timer.id)
        assertTrue((vm.uiState.first() as CookRecipeUiState.Content).timers.isEmpty())
    }

    @Test
    fun `adjusting a timer never drops its total below one minute`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Rice"))
        val vm = CookRecipeViewModel(handleFor("r1"), repository, newCookTimerRepository())
        vm.uiState.first { it is CookRecipeUiState.Content }
        vm.addTimer(minutes = 1, label = "")

        val timer = (vm.uiState.first() as CookRecipeUiState.Content).timers.single()
        vm.adjustTimer(timer.id, deltaMinutes = -5)

        assertEquals(60, (vm.uiState.first() as CookRecipeUiState.Content).timers.single().totalSeconds)
    }

    @Test
    fun `pausing and resuming a timer`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Rice"))
        val vm = CookRecipeViewModel(handleFor("r1"), repository, newCookTimerRepository())
        vm.uiState.first { it is CookRecipeUiState.Content }
        vm.addTimer(minutes = 5, label = "")
        val timer = (vm.uiState.first() as CookRecipeUiState.Content).timers.single()

        vm.toggleTimer(timer.id)
        assertFalse((vm.uiState.first() as CookRecipeUiState.Content).timers.single().isRunning)

        vm.toggleTimer(timer.id)
        assertTrue((vm.uiState.first() as CookRecipeUiState.Content).timers.single().isRunning)
    }

    @Test
    fun `a timer from another recipe does not show up here`() = runTest {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Rice"))
        repository.saveRecipe(RecipeDraft(id = "r2", title = "Eggs"))
        val sharedTimers = newCookTimerRepository()
        val vm1 = CookRecipeViewModel(handleFor("r1"), repository, sharedTimers)
        val vm2 = CookRecipeViewModel(handleFor("r2"), repository, sharedTimers)
        vm1.uiState.first { it is CookRecipeUiState.Content }
        vm2.uiState.first { it is CookRecipeUiState.Content }

        vm1.addTimer(minutes = 5, label = "Rice timer")

        assertEquals(1, (vm1.uiState.first() as CookRecipeUiState.Content).timers.size)
        assertTrue((vm2.uiState.first() as CookRecipeUiState.Content).timers.isEmpty())
    }

    // Shares mainDispatcherRule's own scheduler with runTest, so advanceTimeBy actually moves the
    // virtual clock the ticking coroutine (launched on Dispatchers.Main, inside CookTimerRepository)
    // is delay()ing against.
    @Test
    fun `a running timer counts down and stops at zero`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeRecipeRepository()
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Egg"))
        val vm = CookRecipeViewModel(handleFor("r1"), repository, newCookTimerRepository())
        vm.uiState.first { it is CookRecipeUiState.Content }

        vm.addTimer(minutes = 1, label = "Egg")

        advanceTimeBy(3_000)
        runCurrent()
        var timer = (vm.uiState.first() as CookRecipeUiState.Content).timers.single()
        assertEquals(57, timer.remainingSeconds)
        assertTrue(timer.isRunning)

        advanceTimeBy(60_000)
        runCurrent()
        timer = (vm.uiState.first() as CookRecipeUiState.Content).timers.single()
        assertEquals(0, timer.remainingSeconds)
        assertFalse(timer.isRunning)
    }
}
