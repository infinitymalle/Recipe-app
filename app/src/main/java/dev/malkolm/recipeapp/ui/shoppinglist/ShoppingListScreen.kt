package dev.malkolm.recipeapp.ui.shoppinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.model.ShoppingListItem
import dev.malkolm.recipeapp.ui.components.BlurredImageBackground
import dev.malkolm.recipeapp.ui.components.PickRecipeDialog
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import java.time.Instant

/** Stateful entry point: connects the ViewModel to the stateless [ShoppingListContent]. */
@Composable
fun ShoppingListScreen(onBack: () -> Unit, viewModel: ShoppingListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingListContent(
        uiState = uiState,
        onBack = onBack,
        onAddItem = viewModel::addItem,
        onAddFromRecipe = viewModel::addFromRecipe,
        onSetChecked = viewModel::setChecked,
        onRemoveItem = viewModel::removeItem,
        onClearChecked = viewModel::clearChecked
    )
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListContent(
    uiState: ShoppingListUiState,
    onBack: () -> Unit,
    onAddItem: (name: String, amount: String) -> Unit,
    onAddFromRecipe: (recipeId: String) -> Unit,
    onSetChecked: (id: String, checked: Boolean) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearChecked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddItem by remember { mutableStateOf(false) }
    var showPickRecipe by remember { mutableStateOf(false) }

    BlurredImageBackground(imagePath = uiState.backgroundImagePath, modifier = modifier) {
        Scaffold(
            containerColor =
                if (uiState.backgroundImagePath != null) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.shopping_list_title)) },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.recipe_detail_back)) }
                    },
                    actions = {
                        if (uiState.items.any { it.isChecked }) {
                            TextButton(onClick = onClearChecked) {
                                Text(stringResource(R.string.shopping_list_clear_checked))
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.shopping_list_empty))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ShoppingListRow(
                            item = item,
                            onSetChecked = { checked -> onSetChecked(item.id, checked) },
                            onRemove = { onRemoveItem(item.id) }
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { showAddItem = true }) {
                                Text(stringResource(R.string.shopping_list_add_item))
                            }
                            OutlinedButton(onClick = { showPickRecipe = true }) {
                                Text(stringResource(R.string.shopping_list_add_from_recipe))
                            }
                        }
                    }
                }
            }

            // Also reachable from the empty state, where the LazyColumn above is not shown.
            if (uiState.items.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddItem = true }) {
                            Text(stringResource(R.string.shopping_list_add_item))
                        }
                        OutlinedButton(onClick = { showPickRecipe = true }) {
                            Text(stringResource(R.string.shopping_list_add_from_recipe))
                        }
                    }
                }
            }
        }
    }

    if (showAddItem) {
        AddShoppingItemDialog(
            onDismiss = { showAddItem = false },
            onAdd = { name, amount ->
                onAddItem(name, amount)
                showAddItem = false
            }
        )
    }

    if (showPickRecipe) {
        PickRecipeDialog(
            title = stringResource(R.string.shopping_list_pick_recipe_title),
            recipes = uiState.recipes,
            onDismiss = { showPickRecipe = false },
            onPick = { recipeId ->
                onAddFromRecipe(recipeId)
                showPickRecipe = false
            }
        )
    }
}

@Composable
private fun ShoppingListRow(item: ShoppingListItem, onSetChecked: (Boolean) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = onSetChecked)
        val label = if (item.amount.isNullOrBlank()) item.name else "${item.name} (${item.amount})"
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
            color =
                if (item.isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
        )
        TextButton(onClick = onRemove) { Text(stringResource(R.string.shopping_list_remove)) }
    }
}

@Composable
private fun AddShoppingItemDialog(onDismiss: () -> Unit, onAdd: (name: String, amount: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shopping_list_add_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.shopping_list_item_name)) }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.shopping_list_item_amount)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, amount) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.recipe_edit_dialog_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListContentPreview() {
    RecipeAppTheme {
        ShoppingListContent(
            uiState =
                ShoppingListUiState(
                    items =
                        listOf(
                            ShoppingListItem("1", "Eggs", "6", isChecked = false, addedAt = Instant.now()),
                            ShoppingListItem("2", "Flour", "2 cups", isChecked = true, addedAt = Instant.now())
                        ),
                    recipes =
                        listOf(
                            RecipeSummary(
                                id = "r1",
                                title = "Pancakes",
                                servings = null,
                                cookingTimeMinutes = null,
                                rating = null,
                                thumbnailPath = null,
                                updatedAt = Instant.now()
                            )
                        )
                ),
            onBack = {},
            onAddItem = { _, _ -> },
            onAddFromRecipe = {},
            onSetChecked = { _, _ -> },
            onRemoveItem = {},
            onClearChecked = {}
        )
    }
}
