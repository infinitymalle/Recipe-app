package dev.malkolm.recipeapp.ui.recipelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.domain.repository.TagRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Everything the recipe list screen can show. The screen renders this and nothing else. */
sealed interface RecipeListUiState {
    /** The repository has not emitted yet. */
    data object Loading : RecipeListUiState

    data class Content(
        val recipes: List<RecipeSummary>,
        val availableTags: List<Tag>,
        val selectedTagId: String?,
        val searchQuery: String
    ) : RecipeListUiState
}

/**
 * Holds the screen's state across configuration changes (rotation, dark-mode switch).
 * State flows down to the UI as [uiState]; user actions flow up as function calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeListViewModel
@Inject
constructor(
    private val recipeRepository: RecipeRepository,
    tagRepository: TagRepository
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedTagId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecipeListUiState> =
        combine(searchQuery, selectedTagId) { query, tagId -> query to tagId }
            .flatMapLatest { (query, tagId) ->
                combine(
                    recipeRepository.observeRecipeSummaries(searchQuery = query, tagId = tagId),
                    tagRepository.observeTagsInUse()
                ) { recipes, tags ->
                    RecipeListUiState.Content(
                        recipes = recipes,
                        availableTags = tags,
                        selectedTagId = tagId,
                        searchQuery = query
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipeListUiState.Loading)

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    /** Tapping the already-selected tag clears the filter. */
    fun selectTag(tagId: String) {
        selectedTagId.value = if (selectedTagId.value == tagId) null else tagId
    }
}
