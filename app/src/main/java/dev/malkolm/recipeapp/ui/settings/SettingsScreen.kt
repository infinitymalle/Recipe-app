package dev.malkolm.recipeapp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import kotlinx.coroutines.launch

/** Stateful entry point: connects the ViewModel to the stateless [SettingsContent]. */
@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportSucceeded = stringResource(R.string.settings_export_success_prefix)
    val importSucceeded = stringResource(R.string.settings_import_success_prefix)
    val failed = stringResource(R.string.settings_backup_error)

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val result = viewModel.exportBackup(uri)
                val message = result.fold({ count -> "$exportSucceeded $count" }, { failed })
                snackbarHostState.showSnackbar(message)
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val result = viewModel.importBackup(uri)
                val message = result.fold({ count -> "$importSucceeded $count" }, { failed })
                snackbarHostState.showSnackbar(message)
            }
        }

    SettingsContent(
        onBack = onBack,
        onExport = { exportLauncher.launch("recipe-app-backup.zip") },
        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/** Stateless: the picker launches and the ViewModel call happen in [SettingsScreen] above this. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.recipe_detail_back)) }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings_backup_description))
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_export_backup))
            }
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_import_backup))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    RecipeAppTheme {
        SettingsContent(onBack = {}, onExport = {}, onImport = {})
    }
}
