package dev.malkolm.recipeapp.ui.recipelist

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeListViewModelTest {
    @Test
    fun `initial state is empty`() {
        val viewModel = RecipeListViewModel()

        assertEquals(RecipeListUiState.Empty, viewModel.uiState.value)
    }
}
