package dev.malkolm.recipeapp.ui.cookrecipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.timer.CookTimerRepository
import dev.malkolm.recipeapp.timer.RunningCookTimer
import dev.malkolm.recipeapp.ui.navigation.CookRecipeRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A running or paused countdown, e.g. for "brown rice" while a recipe's own time suits white rice. */
data class CookTimer(
    val id: String,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean
)

/** Everything the cook-along screen can show. The screen renders this and nothing else. */
sealed interface CookRecipeUiState {
    data object Loading : CookRecipeUiState

    data object NotFound : CookRecipeUiState

    data class Content(
        val title: String,
        val steps: List<String>,
        val currentStepIndex: Int,
        /** From the recipe's own cooking time, offered as a starting point for "+ Add timer". */
        val suggestedTimerMinutes: Int?,
        val timers: List<CookTimer> = emptyList()
    ) : CookRecipeUiState
}

/**
 * Timers themselves live in [CookTimerRepository] (app-wide, not tied to this screen or even this
 * recipe): this ViewModel just reads the ones for [recipeId] and forwards actions to it. That is
 * what lets a timer keep running after you leave this screen. See [CookTimerRepository]'s doc for
 * what "keeps running" does and does not cover.
 */
@HiltViewModel
class CookRecipeViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    recipeRepository: RecipeRepository,
    private val cookTimerRepository: CookTimerRepository
) : ViewModel() {
    private val recipeId = savedStateHandle.toRoute<CookRecipeRoute>().recipeId
    private val recipeTitle = MutableStateFlow("")

    private val shell = MutableStateFlow<CookRecipeUiState>(CookRecipeUiState.Loading)
    private val currentStepIndex = MutableStateFlow(0)

    val uiState: StateFlow<CookRecipeUiState> =
        combine(shell, cookTimerRepository.timers, currentStepIndex) { shellState, allTimers, stepIndex ->
            when (shellState) {
                is CookRecipeUiState.Content ->
                    shellState.copy(
                        currentStepIndex = stepIndex,
                        timers = allTimers.filter { it.recipeId == recipeId }.map { it.toCookTimer() }
                    )

                else -> shellState
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CookRecipeUiState.Loading)

    init {
        viewModelScope.launch {
            val recipe = recipeRepository.observeRecipe(recipeId).first()
            if (recipe == null) {
                shell.value = CookRecipeUiState.NotFound
            } else {
                recipeTitle.value = recipe.title
                shell.value =
                    CookRecipeUiState.Content(
                        title = recipe.title,
                        steps = stepsFrom(recipe.notes),
                        currentStepIndex = 0,
                        suggestedTimerMinutes = recipe.cookingTimeMinutes
                    )
            }
        }
    }

    fun nextStep() {
        val steps = (shell.value as? CookRecipeUiState.Content)?.steps ?: return
        currentStepIndex.value = (currentStepIndex.value + 1).coerceAtMost(steps.lastIndex.coerceAtLeast(0))
    }

    fun previousStep() {
        currentStepIndex.value = (currentStepIndex.value - 1).coerceAtLeast(0)
    }

    fun addTimer(minutes: Int, label: String) =
        cookTimerRepository.addTimer(recipeId, recipeTitle.value, label, minutes)

    fun adjustTimer(id: String, deltaMinutes: Int) = cookTimerRepository.adjustTimer(id, deltaMinutes)

    fun toggleTimer(id: String) = cookTimerRepository.toggleTimer(id)

    fun removeTimer(id: String) = cookTimerRepository.removeTimer(id)
}

private fun RunningCookTimer.toCookTimer() = CookTimer(
    id = id,
    label = label,
    totalSeconds = totalSeconds,
    remainingSeconds = remainingSeconds,
    isRunning = isRunning
)

/** Notes split into steps on blank lines; the whole thing is one step if there are none. */
private val blankLine = Regex("\n\\s*\n")

fun stepsFrom(notes: String): List<String> = notes
    .split(blankLine)
    .map { it.trim() }
    .filter { it.isNotEmpty() }
