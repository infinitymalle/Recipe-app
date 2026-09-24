package dev.malkolm.recipeapp.ui.recipelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import java.time.Instant

/** Stateful entry point: connects the ViewModel to the stateless [RecipeListContent]. */
@Composable
fun RecipeListScreen(
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListContent(
        uiState = uiState,
        onOpenRecipe = onOpenRecipe,
        onAddRecipe = onAddRecipe,
        onOpenSettings = onOpenSettings
    )
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListContent(
    uiState: RecipeListUiState,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.recipe_list_settings), style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddRecipe) {
                Text(stringResource(R.string.recipe_list_add))
            }
        }
    ) { innerPadding ->
        when (uiState) {
            RecipeListUiState.Loading -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding))

            RecipeListUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.recipe_list_empty))
                }
            }

            is RecipeListUiState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recipes, key = { it.id }) { recipe ->
                        RecipeSummaryCard(recipe = recipe, onClick = { onOpenRecipe(recipe.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeSummaryCard(recipe: RecipeSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = recipe.title, style = MaterialTheme.typography.titleMedium)
            val details = recipeDetailsLine(recipe)
            if (details != null) {
                Text(text = details, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun recipeDetailsLine(recipe: RecipeSummary): String? {
    val parts = mutableListOf<String>()
    recipe.servings?.let { parts += pluralStringResource(R.plurals.recipe_servings_short, it, it) }
    recipe.cookingTimeMinutes?.let { parts += stringResource(R.string.recipe_cooking_time_short, it) }
    recipe.rating?.let { parts += stringResource(R.string.recipe_rating_short, it) }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@Preview(showBackground = true)
@Composable
private fun RecipeListContentEmptyPreview() {
    RecipeAppTheme {
        RecipeListContent(
            uiState = RecipeListUiState.Empty,
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeListContentPreview() {
    RecipeAppTheme {
        RecipeListContent(
            uiState =
                RecipeListUiState.Content(
                    recipes =
                        listOf(
                            RecipeSummary(
                                id = "1",
                                title = "Sourdough bread",
                                servings = 4,
                                cookingTimeMinutes = 45,
                                rating = 5,
                                thumbnailPath = null,
                                updatedAt = Instant.now()
                            )
                        )
                ),
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {}
        )
    }
}
