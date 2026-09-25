package dev.malkolm.recipeapp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Stateful entry point: connects the ViewModel to the stateless [SettingsContent]. */
@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val backgroundBlur by viewModel.backgroundBlur.collectAsStateWithLifecycle()
    val shoppingListImagePath by viewModel.shoppingListImagePath.collectAsStateWithLifecycle()
    val exportSucceeded = stringResource(R.string.settings_export_success_prefix)
    val importSucceeded = stringResource(R.string.settings_import_success_prefix)
    val failed = stringResource(R.string.settings_backup_error)
    val pictureFailed = stringResource(R.string.recipe_edit_picture_error)
    val examplesAdded = stringResource(R.string.settings_examples_added_prefix)

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
    val pickShoppingListImage =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                viewModel.setShoppingListImage(uri).onFailure { snackbarHostState.showSnackbar(pictureFailed) }
            }
        }

    SettingsContent(
        onBack = onBack,
        themeMode = themeMode,
        onThemeModeChange = viewModel::setThemeMode,
        backgroundBlur = backgroundBlur,
        onBackgroundBlurChange = viewModel::setBackgroundBlur,
        shoppingListImagePath = shoppingListImagePath,
        onChooseShoppingListImage = {
            pickShoppingListImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemoveShoppingListImage = { scope.launch { viewModel.removeShoppingListImage() } },
        onAddExampleRecipes = {
            scope.launch {
                val result = viewModel.addExampleRecipes()
                snackbarHostState.showSnackbar(result.fold({ count -> "$examplesAdded $count" }, { failed }))
            }
        },
        onExport = { exportLauncher.launch("recipe-app-backup.zip") },
        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/** Stateless: the picker launches and the ViewModel calls happen in [SettingsScreen] above this. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    onBack: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    backgroundBlur: Int,
    onBackgroundBlurChange: (Int) -> Unit,
    shoppingListImagePath: String?,
    onChooseShoppingListImage: () -> Unit,
    onRemoveShoppingListImage: () -> Unit,
    onAddExampleRecipes: () -> Unit,
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModeChip(ThemeMode.SYSTEM, R.string.settings_theme_system, themeMode, onThemeModeChange)
                ThemeModeChip(ThemeMode.LIGHT, R.string.settings_theme_light, themeMode, onThemeModeChange)
                ThemeModeChip(ThemeMode.DARK, R.string.settings_theme_dark, themeMode, onThemeModeChange)
            }

            HorizontalDivider()
            Text(stringResource(R.string.settings_blur_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.settings_blur_value, backgroundBlur))
            val blurRange = ThemeSettingsRepository.BLUR_RANGE
            Slider(
                value = backgroundBlur.toFloat(),
                onValueChange = { onBackgroundBlurChange(it.roundToInt()) },
                valueRange = blurRange.first.toFloat()..blurRange.last.toFloat(),
                steps = blurRange.last - blurRange.first - 1
            )

            HorizontalDivider()
            Text(stringResource(R.string.settings_shopping_picture_title), style = MaterialTheme.typography.titleSmall)
            // Doubles as a live preview of the blur slider above.
            if (shoppingListImagePath != null) {
                AsyncImage(
                    model = File(LocalContext.current.filesDir, shoppingListImagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .blur(backgroundBlur.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(R.string.settings_shopping_picture_none))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChooseShoppingListImage, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            if (shoppingListImagePath == null) {
                                R.string.settings_shopping_picture_choose
                            } else {
                                R.string.settings_shopping_picture_change
                            }
                        )
                    )
                }
                if (shoppingListImagePath != null) {
                    OutlinedButton(onClick = onRemoveShoppingListImage, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.recipe_edit_remove_attachment))
                    }
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.settings_examples_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.settings_examples_description))
            OutlinedButton(onClick = onAddExampleRecipes, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_examples_add))
            }

            HorizontalDivider()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeChip(mode: ThemeMode, labelRes: Int, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    FilterChip(selected = mode == selected, onClick = { onSelect(mode) }, label = { Text(stringResource(labelRes)) })
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    RecipeAppTheme {
        SettingsContent(
            onBack = {},
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChange = {},
            backgroundBlur = ThemeSettingsRepository.DEFAULT_BLUR,
            onBackgroundBlurChange = {},
            shoppingListImagePath = null,
            onChooseShoppingListImage = {},
            onRemoveShoppingListImage = {},
            onAddExampleRecipes = {},
            onExport = {},
            onImport = {}
        )
    }
}
