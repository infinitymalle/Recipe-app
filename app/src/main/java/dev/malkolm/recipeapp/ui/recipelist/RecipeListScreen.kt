package dev.malkolm.recipeapp.ui.recipelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme

/** Stateful entry point: connects the ViewModel to the stateless [RecipeListContent]. */
@Composable
fun RecipeListScreen(viewModel: RecipeListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListContent(uiState = uiState)
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListContent(uiState: RecipeListUiState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { innerPadding ->
        when (uiState) {
            RecipeListUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.recipe_list_empty))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeListContentEmptyPreview() {
    RecipeAppTheme {
        RecipeListContent(uiState = RecipeListUiState.Empty)
    }
}
