package dev.malkolm.recipeapp.ui.recipelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.ui.components.BlurredImageBackground
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import java.io.File
import java.time.Instant

/** Stateful entry point: connects the ViewModel to the stateless [RecipeListContent]. */
@Composable
fun RecipeListScreen(
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShoppingList: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()

    // Picked the first time recipes with a picture are seen, then kept while searching/filtering.
    // Plain remember (not rememberSaveable) on purpose: this screen leaves composition while
    // another one is open, so coming back to the list picks a new random picture every time.
    var backgroundImagePath by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState) {
        if (backgroundImagePath == null) {
            val content = uiState as? RecipeListUiState.Content
            backgroundImagePath = content?.recipes?.mapNotNull { it.thumbnailPath }?.randomOrNull()
        }
    }

    RecipeListContent(
        uiState = uiState,
        searchText = searchText,
        backgroundImagePath = backgroundImagePath,
        onOpenRecipe = onOpenRecipe,
        onAddRecipe = onAddRecipe,
        onOpenSettings = onOpenSettings,
        onOpenShoppingList = onOpenShoppingList,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onSelectTag = viewModel::selectTag
    )
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListContent(
    uiState: RecipeListUiState,
    searchText: String,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShoppingList: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundImagePath: String? = null
) {
    BlurredImageBackground(imagePath = backgroundImagePath, modifier = modifier) {
        Scaffold(
            containerColor = if (backgroundImagePath !=
                null
            ) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.background
            },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        TextButton(onClick = onOpenSettings) {
                            Text(
                                stringResource(R.string.recipe_list_settings),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExtendedFloatingActionButton(onClick = onOpenShoppingList) {
                        Text(stringResource(R.string.recipe_list_shopping_list))
                    }
                    ExtendedFloatingActionButton(onClick = onAddRecipe) {
                        Text(stringResource(R.string.recipe_list_add))
                    }
                }
            }
        ) { innerPadding ->
            when (uiState) {
                RecipeListUiState.Loading -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding))

                is RecipeListUiState.Content -> {
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = onSearchQueryChange,
                            label = { Text(stringResource(R.string.recipe_list_search_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                        if (uiState.availableTags.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.availableTags, key = { it.id }) { tag ->
                                    FilterChip(
                                        selected = tag.id == uiState.selectedTagId,
                                        onClick = { onSelectTag(tag.id) },
                                        label = { Text(tag.name) }
                                    )
                                }
                            }
                        }
                        if (uiState.recipes.isEmpty()) {
                            val isFiltered = uiState.searchQuery.isNotBlank() || uiState.selectedTagId != null
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(
                                        if (isFiltered) R.string.recipe_list_no_matches else R.string.recipe_list_empty
                                    )
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
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
        }
    }
}

@Composable
private fun RecipeSummaryCard(recipe: RecipeSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recipe.thumbnailPath != null) {
                val context = LocalContext.current
                AsyncImage(
                    model = File(context.filesDir, recipe.thumbnailPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Column {
                Text(text = recipe.title, style = MaterialTheme.typography.titleMedium)
                val details = recipeDetailsLine(recipe)
                if (details != null) {
                    Text(text = details, style = MaterialTheme.typography.bodyMedium)
                }
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
            uiState =
                RecipeListUiState.Content(
                    recipes = emptyList(),
                    availableTags = emptyList(),
                    selectedTagId = null,
                    searchQuery = ""
                ),
            searchText = "",
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {},
            onOpenShoppingList = {},
            onSearchQueryChange = {},
            onSelectTag = {}
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
                        ),
                    availableTags = listOf(Tag.of("t1", "Baking"), Tag.of("t2", "Breakfast")),
                    selectedTagId = "t1",
                    searchQuery = ""
                ),
            searchText = "",
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {},
            onOpenShoppingList = {},
            onSearchQueryChange = {},
            onSelectTag = {}
        )
    }
}
