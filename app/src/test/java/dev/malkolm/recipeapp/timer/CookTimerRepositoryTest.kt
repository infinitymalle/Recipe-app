package dev.malkolm.recipeapp.timer

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.testutil.MainDispatcherRule
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith

/** Uses Robolectric (via [AndroidJUnit4]): the repository needs a real [android.content.Context]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CookTimerRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newRepository() =
        CookTimerRepository(ApplicationProvider.getApplicationContext(), SequentialIdGenerator())

    @Test
    fun `timersFor only returns timers for that recipe`() {
        val repository = newRepository()

        repository.addTimer(recipeId = "r1", recipeTitle = "Rice", label = "Rice", minutes = 5)
        repository.addTimer(recipeId = "r2", recipeTitle = "Eggs", label = "Eggs", minutes = 3)

        assertEquals(listOf("Rice"), repository.timersFor("r1").map { it.label })
        assertEquals(listOf("Eggs"), repository.timersFor("r2").map { it.label })
    }

    @Test
    fun `a timer survives across independent reads, unlike a per-screen ViewModel's own state would`() {
        val repository = newRepository()
        repository.addTimer(recipeId = "r1", recipeTitle = "Rice", label = "", minutes = 10)

        // Simulates leaving and re-entering the Cook Recipe screen: a brand new read of the same
        // shared repository, not a fresh in-memory list.
        assertEquals(1, repository.timersFor("r1").size)
        assertEquals(600, repository.timersFor("r1").single().remainingSeconds)
    }

    @Test
    fun `ticking counts every running timer down and stops each at zero independently`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = newRepository()
            repository.addTimer(recipeId = "r1", recipeTitle = "Rice", label = "Fast", minutes = 1)
            repository.addTimer(recipeId = "r1", recipeTitle = "Rice", label = "Slow", minutes = 2)

            advanceTimeBy(60_500)
            runCurrent()

            val fast = repository.timersFor("r1").single { it.label == "Fast" }
            val slow = repository.timersFor("r1").single { it.label == "Slow" }
            assertEquals(0, fast.remainingSeconds)
            assertTrue(!fast.isRunning)
            assertEquals(60, slow.remainingSeconds)
            assertTrue(slow.isRunning)
        }

    @Test
    fun `removing a timer takes it out of the list immediately`() {
        val repository = newRepository()
        repository.addTimer(recipeId = "r1", recipeTitle = "Rice", label = "", minutes = 5)
        val id = repository.timersFor("r1").single().id

        repository.removeTimer(id)

        assertTrue(repository.timersFor("r1").isEmpty())
    }
}
