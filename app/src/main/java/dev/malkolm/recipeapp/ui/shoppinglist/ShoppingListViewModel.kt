package dev.malkolm.recipeapp.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.model.ShoppingListItem
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.domain.repository.ShoppingListRepository
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val items: List<ShoppingListItem> = emptyList(),
    val recipes: List<RecipeSummary> = emptyList(),
    /** The picture chosen in Settings to show behind the list, or `null` for none. */
    val backgroundImagePath: String? = null
)

@HiltViewModel
class ShoppingListViewModel
@Inject
constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val recipeRepository: RecipeRepository,
    themeSettingsRepository: ThemeSettingsRepository
) : ViewModel() {
    val uiState: StateFlow<ShoppingListUiState> =
        combine(
            shoppingListRepository.observeItems(),
            recipeRepository.observeRecipeSummaries(),
            themeSettingsRepository.shoppingListImagePath
        ) { items, recipes, imagePath ->
            ShoppingListUiState(items = items, recipes = recipes, backgroundImagePath = imagePath)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingListUiState())

    fun addItem(name: String, amount: String) {
        if (name.isBlank()) return
        viewModelScope.launch { shoppingListRepository.addItem(name, amount.takeIf { it.isNotBlank() }) }
    }

    fun setChecked(id: String, checked: Boolean) {
        viewModelScope.launch { shoppingListRepository.setChecked(id, checked) }
    }

    fun removeItem(id: String) {
        viewModelScope.launch { shoppingListRepository.deleteItem(id) }
    }

    fun clearChecked() {
        viewModelScope.launch { shoppingListRepository.clearChecked() }
    }

    fun clearAll() {
        viewModelScope.launch { shoppingListRepository.clearAll() }
    }

    /** Parses the recipe's "Recipe" text (ingredient entries only, section headings skipped) and merges it in. */
    fun addFromRecipe(recipeId: String) {
        viewModelScope.launch {
            val recipe = recipeRepository.observeRecipe(recipeId).first() ?: return@launch
            shoppingListRepository.addEntries(shoppingEntriesFrom(recipe.ingredients))
        }
    }
}
