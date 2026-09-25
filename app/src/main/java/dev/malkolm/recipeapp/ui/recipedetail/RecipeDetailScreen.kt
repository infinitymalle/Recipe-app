package dev.malkolm.recipeapp.ui.recipedetail

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.ui.components.BlurredImageBackground
import dev.malkolm.recipeapp.ui.recipeedit.IngredientListItem
import dev.malkolm.recipeapp.ui.recipeedit.ingredientItemsFrom
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import java.io.File
import java.time.Instant
import kotlinx.coroutines.launch

/** Stateful entry point: connects the ViewModel to the stateless [RecipeDetailContent]. */
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onCookRecipe: (String) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.recipe_detail_deleted_message)
    val undoLabel = stringResource(R.string.recipe_detail_undo)
    val shareErrorMessage = stringResource(R.string.recipe_detail_share_error)

    RecipeDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEditRecipe = onEditRecipe,
        onCookRecipe = onCookRecipe,
        onShare = {
            scope.launch {
                val file = viewModel.shareRecipe()
                if (file == null) {
                    snackbarHostState.showSnackbar(shareErrorMessage)
                } else {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val sendIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
            }
        },
        onConfirmDelete = {
            scope.launch {
                viewModel.deleteRecipe()
                val result =
                    snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreRecipe()
                } else {
                    onBack()
                }
            }
        }
    )
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailContent(
    uiState: RecipeDetailUiState,
    onBack: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onCookRecipe: (String) -> Unit,
    onShare: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val backgroundImagePath = (uiState as? RecipeDetailUiState.Content)?.recipe?.picturePath()

    BlurredImageBackground(imagePath = backgroundImagePath, modifier = modifier) {
        Scaffold(
            containerColor = if (backgroundImagePath !=
                null
            ) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.background
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = (uiState as? RecipeDetailUiState.Content)?.recipe?.title
                                ?: stringResource(R.string.app_name)
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.recipe_detail_back)) }
                    },
                    actions = {
                        if (uiState is RecipeDetailUiState.Content) {
                            TextButton(onClick = { onEditRecipe(uiState.recipe.id) }) {
                                Text(stringResource(R.string.recipe_detail_edit))
                            }
                            TextButton(onClick = onShare) {
                                Text(stringResource(R.string.recipe_detail_share))
                            }
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Text(stringResource(R.string.recipe_detail_delete))
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (uiState) {
                RecipeDetailUiState.Loading -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding))

                RecipeDetailUiState.NotFound -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.recipe_detail_not_found))
                    }
                }

                is RecipeDetailUiState.Content -> {
                    RecipeDetailBody(
                        recipe = uiState.recipe,
                        onCookRecipe = { onCookRecipe(uiState.recipe.id) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.recipe_detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.recipe_detail_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onConfirmDelete()
                    }
                ) { Text(stringResource(R.string.recipe_detail_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.recipe_edit_dialog_cancel))
                }
            }
        )
    }
}

/**
 * The recipe's own picture, the same one the list shows as its thumbnail: the first attachment
 * with a picture (a photo's file, or a link/PDF's saved thumbnail), or `null` if it has none.
 */
private fun Recipe.picturePath(): String? = attachments.firstNotNullOfOrNull { attachment ->
    when (attachment) {
        is Attachment.Image -> attachment.filePath
        is Attachment.Link -> attachment.thumbnailPath
        is Attachment.Pdf -> attachment.thumbnailPath
        is Attachment.Text -> null
    }
}

@Composable
private fun RecipeDetailBody(recipe: Recipe, onCookRecipe: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(onClick = onCookRecipe, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recipe_detail_cook))
            }
        }
        item {
            val facts = recipeFacts(recipe)
            if (facts != null) Text(text = facts, style = MaterialTheme.typography.bodyMedium)
        }
        if (recipe.tags.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag.name) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }
        if (recipe.ingredients.isNotBlank()) {
            item {
                Text(stringResource(R.string.recipe_detail_ingredients), style = MaterialTheme.typography.titleSmall)
            }
            var nextId = 0
            items(ingredientItemsFrom(recipe.ingredients) { (nextId++).toString() }) { item ->
                when (item) {
                    is IngredientListItem.Heading ->
                        Text(
                            item.text,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                    is IngredientListItem.Entry -> {
                        val label = if (item.amount.isNullOrBlank()) item.name else "${item.name} (${item.amount})"
                        Text(label)
                    }
                }
            }
        }
        if (recipe.notes.isNotBlank()) {
            item {
                Column {
                    Text(stringResource(R.string.recipe_detail_notes), style = MaterialTheme.typography.titleSmall)
                    Text(recipe.notes)
                }
            }
        }
        if (recipe.attachments.isNotEmpty()) {
            item {
                Text(stringResource(R.string.recipe_detail_attachments), style = MaterialTheme.typography.titleSmall)
            }
            items(recipe.attachments, key = { it.id }) { attachment -> AttachmentRow(attachment) }
        }
    }
}

@Composable
private fun recipeFacts(recipe: Recipe): String? {
    val parts = mutableListOf<String>()
    recipe.servings?.let { parts += pluralStringResource(R.plurals.recipe_servings_short, it, it) }
    recipe.cookingTimeMinutes?.let { parts += stringResource(R.string.recipe_cooking_time_short, it) }
    recipe.rating?.let { parts += stringResource(R.string.recipe_rating_short, it) }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@Composable
private fun AttachmentRow(attachment: Attachment, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            attachment.title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            when (attachment) {
                is Attachment.Text -> Text(attachment.text)

                is Attachment.Link -> {
                    Text(text = attachment.url, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { uriHandler.openUri(attachment.url) }) {
                        Text(stringResource(R.string.recipe_detail_open_link))
                    }
                }

                is Attachment.Image -> {
                    val context = LocalContext.current
                    AsyncImage(
                        model = File(context.filesDir, attachment.filePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))
                    )
                }

                is Attachment.Pdf -> Text(stringResource(R.string.recipe_attachment_pdf))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeDetailContentPreview() {
    RecipeAppTheme {
        RecipeDetailContent(
            uiState =
                RecipeDetailUiState.Content(
                    recipe =
                        Recipe(
                            id = "1",
                            title = "Sourdough bread",
                            ingredients = "Flour\nWater\nSalt\nStarter",
                            servings = 4,
                            cookingTimeMinutes = 45,
                            rating = 5,
                            notes = "Let it proof overnight.",
                            tags = listOf(Tag.of("t1", "Baking")),
                            attachments = listOf(Attachment.Text(id = "a1", text = "Knead for 10 minutes.")),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now()
                        )
                ),
            onBack = {},
            onEditRecipe = {},
            onCookRecipe = {},
            onShare = {},
            onConfirmDelete = {}
        )
    }
}
