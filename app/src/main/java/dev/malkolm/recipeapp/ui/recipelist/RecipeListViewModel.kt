package dev.malkolm.recipeapp.ui.recipelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Everything the recipe list screen can show. The screen renders this and nothing else. */
sealed interface RecipeListUiState {
    /** The repository has not emitted yet. */
    data object Loading : RecipeListUiState

    /** No recipes saved yet. */
    data object Empty : RecipeListUiState

    data class Content(val recipes: List<RecipeSummary>) : RecipeListUiState
}

/**
 * Holds the screen's state across configuration changes (rotation, dark-mode switch).
 * State flows down to the UI as [uiState]; user actions flow up as function calls.
 */
@HiltViewModel
class RecipeListViewModel
@Inject
constructor(recipeRepository: RecipeRepository) : ViewModel() {
    val uiState: StateFlow<RecipeListUiState> =
        recipeRepository
            .observeRecipeSummaries()
            .map { recipes ->
                if (recipes.isEmpty()) RecipeListUiState.Empty else RecipeListUiState.Content(recipes)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipeListUiState.Loading)
}
