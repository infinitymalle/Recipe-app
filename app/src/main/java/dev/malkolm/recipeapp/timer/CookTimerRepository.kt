package dev.malkolm.recipeapp.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.malkolm.recipeapp.domain.IdGenerator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A cooking timer that keeps running independent of any one screen; see [CookTimerRepository]. */
data class RunningCookTimer(
    val id: String,
    val recipeId: String,
    val recipeTitle: String,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean
)

/**
 * Holds every cook timer across the whole app, so a timer keeps counting down when you leave the
 * Cook Recipe screen, switch to a different recipe, or background the app - not just while one
 * screen's ViewModel happens to be alive.
 *
 * The tick loop runs on [Dispatchers.Main] (via its own long-lived [CoroutineScope], not a
 * ViewModel's), the same entry point [dev.malkolm.recipeapp.testutil.MainDispatcherRule] swaps out
 * in tests, so virtual-time tests can still control it exactly like the old per-screen version did.
 *
 * [CookTimerService] is what actually keeps the process alive and visible (a foreground
 * notification) while a timer runs; this class starts it whenever a timer starts running, and the
 * service decides for itself when to stop, by watching [timers].
 */
@Singleton
class CookTimerRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val idGenerator: IdGenerator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _timers = MutableStateFlow<List<RunningCookTimer>>(emptyList())
    val timers: StateFlow<List<RunningCookTimer>> = _timers.asStateFlow()

    private var tickerJob: Job? = null

    fun timersFor(recipeId: String): List<RunningCookTimer> = _timers.value.filter { it.recipeId == recipeId }

    fun addTimer(recipeId: String, recipeTitle: String, label: String, minutes: Int) {
        if (minutes <= 0) return
        val seconds = minutes * 60
        val timer =
            RunningCookTimer(
                id = idGenerator.newId(),
                recipeId = recipeId,
                recipeTitle = recipeTitle,
                label = label.trim(),
                totalSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = true
            )
        _timers.value += timer
        onTimerStarted()
    }

    /** [deltaMinutes] can be negative; a timer never adjusts below one minute total. */
    fun adjustTimer(id: String, deltaMinutes: Int) {
        update { timer ->
            if (timer.id != id) return@update timer
            val newTotal = (timer.totalSeconds + deltaMinutes * 60).coerceAtLeast(60)
            val newRemaining = (timer.remainingSeconds + deltaMinutes * 60).coerceIn(0, newTotal)
            timer.copy(totalSeconds = newTotal, remainingSeconds = newRemaining)
        }
    }

    fun toggleTimer(id: String) {
        var resumed = false
        update { timer ->
            if (timer.id != id) return@update timer
            val nowRunning = !timer.isRunning && timer.remainingSeconds > 0
            if (nowRunning) resumed = true
            timer.copy(isRunning = nowRunning)
        }
        if (resumed) onTimerStarted()
    }

    fun removeTimer(id: String) {
        _timers.value = _timers.value.filterNot { it.id == id }
    }

    private fun update(transform: (RunningCookTimer) -> RunningCookTimer) {
        _timers.value = _timers.value.map(transform)
    }

    private fun onTimerStarted() {
        ContextCompat.startForegroundService(context, Intent(context, CookTimerService::class.java))
        ensureTicking()
    }

    private fun ensureTicking() {
        if (tickerJob?.isActive == true) return
        tickerJob =
            scope.launch {
                while (isActive) {
                    delay(1000)
                    var anyRunning = false
                    update { timer ->
                        if (!timer.isRunning || timer.remainingSeconds <= 0) return@update timer
                        val remaining = timer.remainingSeconds - 1
                        if (remaining > 0) anyRunning = true
                        timer.copy(remainingSeconds = remaining, isRunning = remaining > 0)
                    }
                    if (!anyRunning) break
                }
            }
    }
}
