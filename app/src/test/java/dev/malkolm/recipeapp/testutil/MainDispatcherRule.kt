package dev.malkolm.recipeapp.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Makes `viewModelScope` (which uses `Dispatchers.Main`) run on a test dispatcher.
 *
 * Unconfined, not standard: it is set up in [starting], before `runTest` creates its own test
 * scheduler, so there is no scheduler to share. Unconfined runs coroutines eagerly instead of
 * needing one, which is what lets `viewModelScope.launch { ... }` inside `init` blocks and
 * `stateIn` observably progress in a plain `runTest { }` body.
 *
 * A test that needs to advance virtual time past a `delay(...)` running on `Dispatchers.Main`
 * (e.g. a ticking timer) should pass [dispatcher] to `runTest(dispatcher) { ... }` itself, so both
 * share one scheduler instead of two independent ones that never see each other's time advance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
