package dev.malkolm.recipeapp.ui.recipedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.ui.navigation.RecipeDetailRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Everything the recipe detail screen can show. The screen renders this and nothing else. */
sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState

    /** The recipe does not exist, or was deleted. */
    data object NotFound : RecipeDetailUiState

    data class Content(val recipe: Recipe) : RecipeDetailUiState
}

@HiltViewModel
class RecipeDetailViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    private val recipeId = savedStateHandle.toRoute<RecipeDetailRoute>().recipeId

    val uiState: StateFlow<RecipeDetailUiState> =
        recipeRepository
            .observeRecipe(recipeId)
            .map { recipe -> if (recipe == null) RecipeDetailUiState.NotFound else RecipeDetailUiState.Content(recipe) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipeDetailUiState.Loading)

    /** Suspends until the delete is written, so the caller can navigate back right after. */
    suspend fun deleteRecipe() = recipeRepository.deleteRecipe(recipeId)

    /** Undoes [deleteRecipe]. */
    suspend fun restoreRecipe() = recipeRepository.restoreRecipe(recipeId)
}
