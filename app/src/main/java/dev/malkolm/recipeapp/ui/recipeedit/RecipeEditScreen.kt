package dev.malkolm.recipeapp.ui.recipeedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.data.RecipeImageStorage
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import java.io.File
import kotlinx.coroutines.launch

/** Stateful entry point: connects the ViewModel to the stateless [RecipeEditContent]. */
@Composable
fun RecipeEditScreen(onSaved: () -> Unit, onCancel: () -> Unit, viewModel: RecipeEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saved = (uiState as? RecipeEditUiState.Editing)?.saved == true
    val snackbarHostState = remember { SnackbarHostState() }
    val ingredientAddedMessage = stringResource(R.string.recipe_edit_ingredient_added)
    val pictureErrorMessage = stringResource(R.string.recipe_edit_picture_error)
    var isErrorSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (saved) onSaved()
    }
    LaunchedEffect(viewModel) {
        viewModel.ingredientAdded.collect {
            isErrorSnackbar = false
            snackbarHostState.showSnackbar(ingredientAddedMessage, duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.pictureError.collect {
            isErrorSnackbar = true
            snackbarHostState.showSnackbar(pictureErrorMessage, duration = SnackbarDuration.Short)
        }
    }

    RecipeEditContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        isErrorSnackbar = isErrorSnackbar,
        onCancel = onCancel,
        onTitleChange = viewModel::updateTitle,
        onAddIngredient = viewModel::addIngredient,
        onRemoveIngredient = viewModel::removeIngredient,
        onSetServings = viewModel::setServings,
        onClearServings = viewModel::clearServings,
        onSetCookingTime = viewModel::setCookingTime,
        onClearCookingTime = viewModel::clearCookingTime,
        onRatingChange = viewModel::setRating,
        onNotesChange = viewModel::updateNotes,
        onTagsChange = viewModel::updateTagsText,
        onAddTextAttachment = viewModel::addTextAttachment,
        onAddLinkAttachment = viewModel::addLinkAttachment,
        onAddPictureAttachment = { uri -> viewModel.addPictureAttachment(uri) },
        onAddCoverPicture = { uri -> viewModel.addCoverPicture(uri) },
        onPrepareCameraCapture = viewModel::prepareCameraCapture,
        onAddPictureAttachmentFile = { path -> viewModel.addPictureAttachment(path) },
        onAddCoverPictureFile = { path -> viewModel.addCoverPicture(path) },
        onRemoveAttachment = viewModel::removeAttachment,
        onSave = viewModel::save
    )
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditContent(
    uiState: RecipeEditUiState,
    onCancel: () -> Unit,
    onTitleChange: (String) -> Unit,
    onAddIngredient: (name: String, amount: String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onSetServings: (String) -> Unit,
    onClearServings: () -> Unit,
    onSetCookingTime: (String) -> Unit,
    onClearCookingTime: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onAddTextAttachment: (title: String, text: String) -> Unit,
    onAddLinkAttachment: (title: String, url: String) -> Unit,
    onAddPictureAttachment: (Uri) -> Unit,
    onAddCoverPicture: (Uri) -> Unit,
    onPrepareCameraCapture: suspend () -> RecipeImageStorage.CaptureTarget,
    onAddPictureAttachmentFile: (String) -> Unit,
    onAddCoverPictureFile: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isErrorSnackbar: Boolean = false
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                if (isErrorSnackbar) {
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState is RecipeEditUiState.Editing && !uiState.isNew) {
                                R.string.recipe_edit_title_edit
                            } else {
                                R.string.recipe_edit_title_new
                            }
                        )
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) }
                },
                actions = {
                    if (uiState is RecipeEditUiState.Editing) {
                        TextButton(onClick = onSave) { Text(stringResource(R.string.recipe_edit_save)) }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            RecipeEditUiState.Loading -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding))

            is RecipeEditUiState.Editing -> {
                RecipeEditForm(
                    state = uiState,
                    modifier = Modifier.padding(innerPadding),
                    onTitleChange = onTitleChange,
                    onAddIngredient = onAddIngredient,
                    onRemoveIngredient = onRemoveIngredient,
                    onSetServings = onSetServings,
                    onClearServings = onClearServings,
                    onSetCookingTime = onSetCookingTime,
                    onClearCookingTime = onClearCookingTime,
                    onRatingChange = onRatingChange,
                    onNotesChange = onNotesChange,
                    onTagsChange = onTagsChange,
                    onAddTextAttachment = onAddTextAttachment,
                    onAddLinkAttachment = onAddLinkAttachment,
                    onAddPictureAttachment = onAddPictureAttachment,
                    onAddCoverPicture = onAddCoverPicture,
                    onPrepareCameraCapture = onPrepareCameraCapture,
                    onAddPictureAttachmentFile = onAddPictureAttachmentFile,
                    onAddCoverPictureFile = onAddCoverPictureFile,
                    onRemoveAttachment = onRemoveAttachment
                )
            }
        }
    }
}

@Composable
private fun RecipeEditForm(
    state: RecipeEditUiState.Editing,
    onTitleChange: (String) -> Unit,
    onAddIngredient: (name: String, amount: String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onSetServings: (String) -> Unit,
    onClearServings: () -> Unit,
    onSetCookingTime: (String) -> Unit,
    onClearCookingTime: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onAddTextAttachment: (title: String, text: String) -> Unit,
    onAddLinkAttachment: (title: String, url: String) -> Unit,
    onAddPictureAttachment: (Uri) -> Unit,
    onAddCoverPicture: (Uri) -> Unit,
    onPrepareCameraCapture: suspend () -> RecipeImageStorage.CaptureTarget,
    onAddPictureAttachmentFile: (String) -> Unit,
    onAddCoverPictureFile: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddIngredient by remember { mutableStateOf(false) }
    var showAddServings by remember { mutableStateOf(false) }
    var showAddCookingTime by remember { mutableStateOf(false) }
    var showAddText by remember { mutableStateOf(false) }
    var showAddLink by remember { mutableStateOf(false) }
    var showCoverPictureChooser by remember { mutableStateOf(false) }
    var showAttachmentPictureChooser by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var pendingCapture by remember { mutableStateOf<PendingCapture?>(null) }

    val pickCoverPicture =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onAddCoverPicture(uri)
        }
    val pickAttachmentPicture =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onAddPictureAttachment(uri)
        }
    val imageRequest = remember { PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly) }

    val takePhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val pending = pendingCapture
            pendingCapture = null
            if (success && pending != null) {
                if (pending.isCover) {
                    onAddCoverPictureFile(
                        pending.relativePath
                    )
                } else {
                    onAddPictureAttachmentFile(pending.relativePath)
                }
            }
        }

    fun takePhotoFor(isCover: Boolean) {
        scope.launch {
            val target = onPrepareCameraCapture()
            pendingCapture = PendingCapture(target.relativePath, isCover)
            takePhoto.launch(target.uri)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.recipe_edit_field_title)) },
                isError = state.titleError,
                supportingText = {
                    if (state.titleError) Text(stringResource(R.string.recipe_edit_field_title_error))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            val coverPicture = state.addedAttachments.filterIsInstance<Attachment.Image>().firstOrNull()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (coverPicture != null) {
                    AttachmentImage(
                        relativePath = coverPicture.filePath,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                OutlinedButton(
                    onClick = { showCoverPictureChooser = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            if (coverPicture != null) {
                                R.string.recipe_edit_change_recipe_picture
                            } else {
                                R.string.recipe_edit_add_recipe_picture
                            }
                        )
                    )
                }
            }
        }

        item {
            Text(stringResource(R.string.recipe_edit_field_ingredients), style = MaterialTheme.typography.titleSmall)
        }
        items(state.ingredients, key = { it.id }) { ingredient ->
            IngredientRow(ingredient = ingredient, onRemove = { onRemoveIngredient(ingredient.id) })
        }
        item {
            OutlinedButton(onClick = { showAddIngredient = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recipe_edit_add_ingredient))
            }
        }

        item {
            ValueOrAddButtonRow(
                value = state.servings,
                addLabel = stringResource(R.string.recipe_edit_add_servings),
                valueText = { pluralOrServings(it) },
                onAdd = { showAddServings = true },
                onEdit = { showAddServings = true },
                onRemove = onClearServings
            )
        }
        item {
            ValueOrAddButtonRow(
                value = state.cookingTimeMinutes,
                addLabel = stringResource(R.string.recipe_edit_add_cooking_time),
                valueText = { stringResource(R.string.recipe_cooking_time_short, it) },
                onAdd = { showAddCookingTime = true },
                onEdit = { showAddCookingTime = true },
                onRemove = onClearCookingTime
            )
        }

        item {
            Column {
                Text(stringResource(R.string.recipe_edit_field_rating), style = MaterialTheme.typography.titleSmall)
                Row {
                    for (star in 1..5) {
                        val interactionSource = remember { MutableInteractionSource() }
                        Text(
                            text = if (star <= (state.rating ?: 0)) "★" else "☆",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier =
                                Modifier
                                    .padding(end = 4.dp)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onRatingChange(star) }
                                    )
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.recipe_edit_field_notes)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = state.tagsText,
                onValueChange = onTagsChange,
                label = { Text(stringResource(R.string.recipe_edit_field_tags)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(stringResource(R.string.recipe_edit_attachments), style = MaterialTheme.typography.titleSmall)
        }
        items(state.addedAttachments, key = { it.id }) { attachment ->
            AttachmentEditRow(attachment = attachment, onRemove = { onRemoveAttachment(attachment.id) })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAddText = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recipe_edit_add_text))
                }
                OutlinedButton(onClick = { showAddLink = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recipe_edit_add_link))
                }
                OutlinedButton(
                    onClick = { showAttachmentPictureChooser = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.recipe_edit_add_picture))
                }
            }
        }
    }

    if (showAddIngredient) {
        AddIngredientDialog(
            onDismiss = { showAddIngredient = false },
            onAdd = { name, amount ->
                onAddIngredient(name, amount)
                showAddIngredient = false
            }
        )
    }
    if (showAddServings) {
        NumberPromptDialog(
            title = stringResource(R.string.recipe_edit_add_servings),
            label = stringResource(R.string.recipe_edit_servings_label),
            initialValue = state.servings?.toString().orEmpty(),
            onDismiss = { showAddServings = false },
            onConfirm = { text ->
                onSetServings(text)
                showAddServings = false
            }
        )
    }
    if (showAddCookingTime) {
        NumberPromptDialog(
            title = stringResource(R.string.recipe_edit_add_cooking_time),
            label = stringResource(R.string.recipe_edit_cooking_time_label),
            initialValue = state.cookingTimeMinutes?.toString().orEmpty(),
            onDismiss = { showAddCookingTime = false },
            onConfirm = { text ->
                onSetCookingTime(text)
                showAddCookingTime = false
            }
        )
    }
    if (showAddText) {
        AddTextAttachmentDialog(
            onDismiss = { showAddText = false },
            onAdd = { title, text ->
                onAddTextAttachment(title, text)
                showAddText = false
            }
        )
    }
    if (showAddLink) {
        AddLinkAttachmentDialog(
            onDismiss = { showAddLink = false },
            onAdd = { title, url ->
                onAddLinkAttachment(title, url)
                showAddLink = false
            }
        )
    }
    if (showCoverPictureChooser) {
        PictureSourceDialog(
            onDismiss = { showCoverPictureChooser = false },
            onTakePhoto = {
                showCoverPictureChooser = false
                takePhotoFor(isCover = true)
            },
            onChooseFromGallery = {
                showCoverPictureChooser = false
                pickCoverPicture.launch(imageRequest)
            }
        )
    }
    if (showAttachmentPictureChooser) {
        PictureSourceDialog(
            onDismiss = { showAttachmentPictureChooser = false },
            onTakePhoto = {
                showAttachmentPictureChooser = false
                takePhotoFor(isCover = false)
            },
            onChooseFromGallery = {
                showAttachmentPictureChooser = false
                pickAttachmentPicture.launch(imageRequest)
            }
        )
    }
}

/** [relativePath] is remembered up front so the result callback (a bare success flag) knows where to look. */
private data class PendingCapture(val relativePath: String, val isCover: Boolean)

@Composable
private fun AttachmentImage(relativePath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = File(context.filesDir, relativePath),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun PictureSourceDialog(onDismiss: () -> Unit, onTakePhoto: () -> Unit, onChooseFromGallery: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recipe_edit_add_picture)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onTakePhoto) { Text(stringResource(R.string.recipe_edit_take_photo)) }
                TextButton(onClick = onChooseFromGallery) {
                    Text(stringResource(R.string.recipe_edit_choose_from_gallery))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}

@Composable
private fun pluralOrServings(value: Int): String = pluralStringResource(R.plurals.recipe_servings_short, value, value)

/** A removable value row once set, or an "Add X" button when not. Used for servings and cooking time. */
@Composable
private fun ValueOrAddButtonRow(
    value: Int?,
    addLabel: String,
    valueText: @Composable (Int) -> String,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (value == null) {
        OutlinedButton(onClick = onAdd, modifier = modifier.fillMaxWidth()) {
            Text(addLabel)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(valueText(value), modifier = Modifier.padding(top = 8.dp))
                Row {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.recipe_detail_edit)) }
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.recipe_edit_remove_attachment)) }
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(ingredient: IngredientEntry, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val label =
                if (ingredient.amount.isNullOrBlank()) ingredient.name else "${ingredient.name} (${ingredient.amount})"
            Text(label, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = onRemove) { Text(stringResource(R.string.recipe_edit_remove_attachment)) }
        }
    }
}

@Composable
private fun AttachmentEditRow(attachment: Attachment, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                attachment.title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                when (attachment) {
                    is Attachment.Text -> Text(attachment.text)

                    is Attachment.Link -> Text(attachment.url)

                    is Attachment.Image ->
                        AttachmentImage(
                            relativePath = attachment.filePath,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp))
                        )

                    else -> Unit
                }
            }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.recipe_edit_remove_attachment)) }
        }
    }
}

@Composable
private fun AddIngredientDialog(onDismiss: () -> Unit, onAdd: (name: String, amount: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recipe_edit_add_ingredient)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.recipe_edit_ingredient_name)) }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.recipe_edit_ingredient_amount)) }
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

@Composable
private fun NumberPromptDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.toIntOrNull() != null) {
                Text(stringResource(R.string.recipe_edit_dialog_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}

@Composable
private fun AddTextAttachmentDialog(onDismiss: () -> Unit, onAdd: (title: String, text: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recipe_edit_add_text)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.recipe_edit_attachment_title)) }
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.recipe_edit_attachment_text)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title, text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.recipe_edit_dialog_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}

@Composable
private fun AddLinkAttachmentDialog(onDismiss: () -> Unit, onAdd: (title: String, url: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recipe_edit_add_link)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.recipe_edit_attachment_title)) }
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.recipe_edit_attachment_url)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title, url) }, enabled = url.isNotBlank()) {
                Text(stringResource(R.string.recipe_edit_dialog_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}

@Preview(showBackground = true)
@Composable
private fun RecipeEditContentPreview() {
    RecipeAppTheme {
        RecipeEditContent(
            uiState = RecipeEditUiState.Editing(recipeId = "1", isNew = true, title = "Sourdough bread"),
            onCancel = {},
            onTitleChange = {},
            onAddIngredient = { _, _ -> },
            onRemoveIngredient = {},
            onSetServings = {},
            onClearServings = {},
            onSetCookingTime = {},
            onClearCookingTime = {},
            onRatingChange = {},
            onNotesChange = {},
            onTagsChange = {},
            onAddTextAttachment = { _, _ -> },
            onAddLinkAttachment = { _, _ -> },
            onAddPictureAttachment = {},
            onAddCoverPicture = {},
            onPrepareCameraCapture = { RecipeImageStorage.CaptureTarget(Uri.EMPTY, "") },
            onAddPictureAttachmentFile = {},
            onAddCoverPictureFile = {},
            onRemoveAttachment = {},
            onSave = {}
        )
    }
}
