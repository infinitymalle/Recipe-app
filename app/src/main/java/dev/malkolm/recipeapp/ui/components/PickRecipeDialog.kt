package dev.malkolm.recipeapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.RecipeSummary

/** A scrollable list of recipes to choose one from, e.g. for the shopping list or the meal plan. */
@Composable
fun PickRecipeDialog(title: String, recipes: List<RecipeSummary>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (recipes.isEmpty()) {
                Text(stringResource(R.string.shopping_list_pick_recipe_empty))
            } else {
                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    recipes.forEach { recipe ->
                        TextButton(onClick = { onPick(recipe.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(recipe.title, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}
