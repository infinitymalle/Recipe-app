package dev.malkolm.recipeapp.ui.recipeedit

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.data.RecipeImageStorage
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.domain.model.stepsFrom
import dev.malkolm.recipeapp.domain.model.toMethodText
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.ui.navigation.RecipeEditRoute
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Everything the add/edit screen can show. The screen renders this and nothing else. */
sealed interface RecipeEditUiState {
    data object Loading : RecipeEditUiState

    data class Editing(
        val recipeId: String,
        val isNew: Boolean,
        val title: String = "",
        val titleError: Boolean = false,
        val ingredients: List<IngredientListItem> = emptyList(),
        /** The method, one entry per step (stored as paragraphs, see `stepsFrom`). */
        val steps: List<MethodStep> = emptyList(),
        val servings: Int? = null,
        val cookingTimeMinutes: Int? = null,
        val rating: Int? = null,
        val notes: String = "",
        val tagsText: String = "",
        /** Text, link and picture attachments, the kinds this screen can add or remove. */
        val addedAttachments: List<Attachment> = emptyList(),
        /** PDF attachments from a future screen, kept untouched across a save here. */
        val otherAttachments: List<Attachment> = emptyList(),
        val saved: Boolean = false
    ) : RecipeEditUiState
}

/** One step of the method while editing; [id] only keeps list rows stable, it is not stored. */
data class MethodStep(val id: String, val text: String)

@HiltViewModel
class RecipeEditViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val idGenerator: IdGenerator,
    private val imageStorage: RecipeImageStorage
) : ViewModel() {
    private val route = savedStateHandle.toRoute<RecipeEditRoute>()
    private val recipeId = route.recipeId ?: idGenerator.newId()
    private val isNew = route.recipeId == null

    private val _uiState = MutableStateFlow<RecipeEditUiState>(RecipeEditUiState.Loading)
    val uiState: StateFlow<RecipeEditUiState> = _uiState.asStateFlow()

    /** One-shot events for the "Ingredient added" confirmation banner. */
    private val _ingredientAdded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val ingredientAdded: Flow<Unit> = _ingredientAdded.asSharedFlow()

    /** One-shot events for when a picture could not be read (e.g. a revoked permission). */
    private val _pictureError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pictureError: Flow<Unit> = _pictureError.asSharedFlow()

    init {
        when {
            !isNew -> {
                viewModelScope.launch {
                    val recipe = recipeRepository.observeRecipe(recipeId).first()
                    _uiState.value =
                        recipe?.toEditingState() ?: RecipeEditUiState.Editing(recipeId = recipeId, isNew = true)
                }
            }

            route.sharedText != null || route.sharedImageUri != null -> {
                viewModelScope.launch {
                    _uiState.value = sharedEditingState(route.sharedText, route.sharedImageUri)
                }
            }

            else -> _uiState.value = RecipeEditUiState.Editing(recipeId = recipeId, isNew = true)
        }
    }

    /** Turns what another app shared into this new recipe's starting link/text/picture attachment. */
    private suspend fun sharedEditingState(sharedText: String?, sharedImageUri: String?): RecipeEditUiState.Editing {
        val attachments = buildList {
            sharedText?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
                add(
                    if (isLikelyUrl(text)) {
                        Attachment.Link(id = idGenerator.newId(), url = text)
                    } else {
                        Attachment.Text(id = idGenerator.newId(), text = text)
                    }
                )
            }
            sharedImageUri?.let { uriString ->
                trySaveImage(uriString.toUri())?.let { filePath ->
                    add(Attachment.Image(id = idGenerator.newId(), filePath = filePath))
                }
            }
        }
        return RecipeEditUiState.Editing(recipeId = recipeId, isNew = true, addedAttachments = attachments)
    }

    fun updateTitle(value: String) = updateEditing { it.copy(title = value, titleError = false) }

    fun addIngredient(name: String, amount: String) {
        if (name.isBlank()) return
        updateEditing { state ->
            state.copy(
                ingredients =
                    state.ingredients +
                        IngredientListItem.Entry(
                            id = idGenerator.newId(),
                            name = name.trim(),
                            amount = amount.trim().ifBlank {
                                null
                            }
                        )
            )
        }
        _ingredientAdded.tryEmit(Unit)
    }

    /** Adds a section title (e.g. "Mashed potatoes") to group the ingredients under it. */
    fun addIngredientHeading(text: String) {
        if (text.isBlank()) return
        updateEditing { state ->
            state.copy(
                ingredients =
                    state.ingredients + IngredientListItem.Heading(id = idGenerator.newId(), text = text.trim())
            )
        }
    }

    fun removeIngredientItem(id: String) = updateEditing { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.id == id })
    }

    /** [direction] is -1 to move the item up a slot, +1 to move it down; a no-op past either end. */
    fun moveIngredientItem(id: String, direction: Int) = updateEditing { state ->
        state.copy(ingredients = state.ingredients.moved(id, direction))
    }

    fun addStep(text: String) {
        if (text.isBlank()) return
        updateEditing { it.copy(steps = it.steps + MethodStep(idGenerator.newId(), text.trim())) }
    }

    /** Replaces a step's text; clearing it removes the step. */
    fun updateStep(id: String, text: String) = updateEditing { state ->
        state.copy(
            steps =
                if (text.isBlank()) {
                    state.steps.filterNot { it.id == id }
                } else {
                    state.steps.map { if (it.id == id) it.copy(text = text.trim()) else it }
                }
        )
    }

    fun removeStep(id: String) = updateEditing { state -> state.copy(steps = state.steps.filterNot { it.id == id }) }

    /** [direction] is -1 to move the step up, +1 to move it down; a no-op past either end. */
    fun moveStep(id: String, direction: Int) = updateEditing { state ->
        val index = state.steps.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in state.steps.indices) {
            state
        } else {
            state.copy(steps = state.steps.toMutableList().apply { add(target, removeAt(index)) })
        }
    }

    /**
     * For recipes from before the method field, whose steps were written in the notes: turns the
     * notes' paragraphs into steps and clears the notes.
     */
    fun moveNotesToMethod() = updateEditing { state ->
        val moved = stepsFrom(state.notes).map { MethodStep(idGenerator.newId(), it) }
        if (moved.isEmpty()) state else state.copy(steps = state.steps + moved, notes = "")
    }

    fun setServings(text: String) = updateEditing { it.copy(servings = text.toIntOrNull()?.takeIf { n -> n >= 1 }) }

    fun clearServings() = updateEditing { it.copy(servings = null) }

    fun setCookingTime(text: String) = updateEditing {
        it.copy(cookingTimeMinutes = text.toIntOrNull()?.takeIf { n -> n >= 0 })
    }

    fun clearCookingTime() = updateEditing { it.copy(cookingTimeMinutes = null) }

    /** Tapping the already-selected star clears the rating. */
    fun setRating(value: Int) = updateEditing { it.copy(rating = if (it.rating == value) null else value) }

    fun updateNotes(value: String) = updateEditing { it.copy(notes = value) }

    fun updateTagsText(value: String) = updateEditing { it.copy(tagsText = value) }

    fun addTextAttachment(title: String, text: String) {
        if (text.isBlank()) return
        updateEditing { state ->
            state.copy(
                addedAttachments =
                    state.addedAttachments +
                        Attachment.Text(
                            id = idGenerator.newId(),
                            text = text.trim(),
                            title = title.trim().ifBlank {
                                null
                            }
                        )
            )
        }
    }

    fun addLinkAttachment(title: String, url: String) {
        if (url.isBlank()) return
        updateEditing { state ->
            state.copy(
                addedAttachments =
                    state.addedAttachments +
                        Attachment.Link(
                            id = idGenerator.newId(),
                            url = url.trim(),
                            title = title.trim().ifBlank {
                                null
                            }
                        )
            )
        }
    }

    /** Adds a picture to the attachment list. */
    fun addPictureAttachment(uri: Uri) {
        viewModelScope.launch {
            val filePath = trySaveImage(uri) ?: return@launch
            val attachment = Attachment.Image(id = idGenerator.newId(), filePath = filePath)
            updateEditing { state -> state.copy(addedAttachments = state.addedAttachments + attachment) }
        }
    }

    /**
     * Sets the recipe's picture: replaces the current cover (the first picture) in place, or adds
     * one at the front if there is none, so it becomes the recipe's list thumbnail.
     */
    fun addCoverPicture(uri: Uri) {
        viewModelScope.launch {
            val filePath = trySaveImage(uri) ?: return@launch
            setCover(Attachment.Image(id = idGenerator.newId(), filePath = filePath))
        }
    }

    /** Saves a picked photo, or emits [pictureError] and returns `null` if it could not be read. */
    private suspend fun trySaveImage(uri: Uri): String? = try {
        imageStorage.saveImage(recipeId, uri)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        _pictureError.tryEmit(Unit)
        null
    }

    /** Creates the file/URI pair the camera app should write a photo into. */
    suspend fun prepareCameraCapture(): RecipeImageStorage.CaptureTarget = imageStorage.createCaptureTarget(recipeId)

    /** Call once the camera reports it wrote into [relativePath] (from [prepareCameraCapture]). */
    fun addPictureAttachment(relativePath: String) {
        val attachment = Attachment.Image(id = idGenerator.newId(), filePath = relativePath)
        updateEditing { state -> state.copy(addedAttachments = state.addedAttachments + attachment) }
    }

    /** Call once the camera reports it wrote into [relativePath] (from [prepareCameraCapture]). */
    fun addCoverPicture(relativePath: String) =
        setCover(Attachment.Image(id = idGenerator.newId(), filePath = relativePath))

    private fun setCover(cover: Attachment.Image) = updateEditing { state ->
        val index = state.addedAttachments.indexOfFirst { it is Attachment.Image }
        val attachments =
            if (index < 0) {
                listOf(cover) + state.addedAttachments
            } else {
                state.addedAttachments.toMutableList().apply { this[index] = cover }
            }
        state.copy(addedAttachments = attachments)
    }

    fun removeAttachment(id: String) = updateEditing { state ->
        state.copy(addedAttachments = state.addedAttachments.filterNot { it.id == id })
    }

    fun save() {
        val state = _uiState.value as? RecipeEditUiState.Editing ?: return
        if (state.title.isBlank()) {
            _uiState.value = state.copy(titleError = true)
            return
        }
        viewModelScope.launch {
            val draft = state.toDraft()
            recipeRepository.saveRecipe(draft)
            // Replaced or removed photos, and camera shots from cancelled edits, are only now unused.
            val keep = draft.attachments.flatMap { it.filePaths() }.toSet()
            runCatching { imageStorage.deleteUnused(recipeId, keep) }
            updateEditing { it.copy(saved = true) }
        }
    }

    private fun updateEditing(transform: (RecipeEditUiState.Editing) -> RecipeEditUiState.Editing) {
        val current = _uiState.value
        if (current is RecipeEditUiState.Editing) _uiState.value = transform(current)
    }

    private fun Recipe.toEditingState(): RecipeEditUiState.Editing = RecipeEditUiState.Editing(
        recipeId = id,
        isNew = false,
        title = title,
        ingredients = ingredientItemsFrom(ingredients) { idGenerator.newId() },
        steps = stepsFrom(method).map { MethodStep(idGenerator.newId(), it) },
        servings = servings,
        cookingTimeMinutes = cookingTimeMinutes,
        rating = rating,
        notes = notes,
        tagsText = tags.joinToString(", ") { it.name },
        addedAttachments = attachments.filter {
            it is Attachment.Text || it is Attachment.Link || it is Attachment.Image
        },
        otherAttachments = attachments.filterNot {
            it is Attachment.Text || it is Attachment.Link ||
                it is Attachment.Image
        }
    )

    private fun RecipeEditUiState.Editing.toDraft(): RecipeDraft = RecipeDraft(
        id = recipeId,
        title = title.trim(),
        ingredients = ingredients.toIngredientsText(),
        method = steps.map { it.text }.toMethodText(),
        servings = servings,
        cookingTimeMinutes = cookingTimeMinutes,
        rating = rating,
        notes = notes,
        tags =
            tagsText
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { name -> Tag.of(idGenerator.newId(), name) },
        attachments = otherAttachments + addedAttachments
    )
}

private fun Attachment.filePaths(): List<String> = when (this) {
    is Attachment.Image -> listOf(filePath)
    is Attachment.Pdf -> listOfNotNull(filePath, thumbnailPath)
    is Attachment.Link -> listOfNotNull(thumbnailPath)
    is Attachment.Text -> emptyList()
}

/** True when the whole (trimmed) string is a single http(s) URL, not just text that contains one. */
private fun isLikelyUrl(text: String): Boolean {
    if (text.any { it.isWhitespace() }) return false
    val uri = text.toUri()
    return (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
}
