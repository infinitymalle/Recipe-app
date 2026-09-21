package dev.malkolm.recipeapp.ui.recipelist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Everything the recipe list screen can show. The screen renders this and nothing else. */
sealed interface RecipeListUiState {
    /** No recipes saved yet. Phase 3 adds Loading and Content once the repository exists. */
    data object Empty : RecipeListUiState
}

/**
 * Holds the screen's state across configuration changes (rotation, dark-mode switch).
 * State flows down to the UI as [uiState]; user actions flow up as function calls.
 */
@HiltViewModel
class RecipeListViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<RecipeListUiState>(RecipeListUiState.Empty)
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()
}
