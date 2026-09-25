package dev.malkolm.recipeapp.ui.importrecipe

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.data.backup.RecipeBackupService
import dev.malkolm.recipeapp.ui.navigation.ImportRecipeRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the import screen shows while it reads the shared file in the background. */
sealed interface ImportRecipeUiState {
    data object Loading : ImportRecipeUiState

    data object Failed : ImportRecipeUiState

    data class Imported(val recipeId: String) : ImportRecipeUiState
}

@HiltViewModel
class ImportRecipeViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    backupService: RecipeBackupService
) : ViewModel() {
    private val _uiState = MutableStateFlow<ImportRecipeUiState>(ImportRecipeUiState.Loading)
    val uiState: StateFlow<ImportRecipeUiState> = _uiState.asStateFlow()

    init {
        val uri = savedStateHandle.toRoute<ImportRecipeRoute>().uri.toUri()
        viewModelScope.launch {
            val recipeId = runCatching { backupService.importShared(uri) }.getOrNull()
            _uiState.value =
                if (recipeId != null) ImportRecipeUiState.Imported(recipeId) else ImportRecipeUiState.Failed
        }
    }
}
