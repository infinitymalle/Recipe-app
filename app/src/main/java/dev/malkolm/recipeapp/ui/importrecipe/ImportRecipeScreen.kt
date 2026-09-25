package dev.malkolm.recipeapp.ui.importrecipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R

/** Stateful entry point: connects the ViewModel to the stateless [ImportRecipeContent]. */
@Composable
fun ImportRecipeScreen(
    onImported: (recipeId: String) -> Unit,
    onGiveUp: () -> Unit,
    viewModel: ImportRecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ImportRecipeUiState.Imported) onImported(state.recipeId)
    }
    ImportRecipeContent(uiState = uiState, onGiveUp = onGiveUp)
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@Composable
fun ImportRecipeContent(uiState: ImportRecipeUiState, onGiveUp: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            when (uiState) {
                ImportRecipeUiState.Loading, is ImportRecipeUiState.Imported -> CircularProgressIndicator()

                ImportRecipeUiState.Failed -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.import_recipe_failed),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedButton(onClick = onGiveUp) { Text(stringResource(R.string.recipe_detail_back)) }
                    }
                }
            }
        }
    }
}
