package dev.malkolm.recipeapp.ui.recipeedit

import android.net.Uri
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
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.ui.navigation.RecipeEditRoute
import javax.inject.Inject
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
        val ingredients: List<IngredientEntry> = emptyList(),
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

    init {
        if (isNew) {
            _uiState.value = RecipeEditUiState.Editing(recipeId = recipeId, isNew = true)
        } else {
            viewModelScope.launch {
                val recipe = recipeRepository.observeRecipe(recipeId).first()
                _uiState.value =
                    recipe?.toEditingState() ?: RecipeEditUiState.Editing(recipeId = recipeId, isNew = true)
            }
        }
    }

    fun updateTitle(value: String) = updateEditing { it.copy(title = value, titleError = false) }

    fun addIngredient(name: String, amount: String) {
        if (name.isBlank()) return
        updateEditing { state ->
            state.copy(
                ingredients =
                    state.ingredients +
                        IngredientEntry(
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

    fun removeIngredient(id: String) = updateEditing { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.id == id })
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
            val attachment = Attachment.Image(
                id = idGenerator.newId(),
                filePath = imageStorage.saveImage(recipeId, uri)
            )
            updateEditing { state -> state.copy(addedAttachments = state.addedAttachments + attachment) }
        }
    }

    /** Adds a picture at the front of the list, so it becomes the recipe's list thumbnail. */
    fun addCoverPicture(uri: Uri) {
        viewModelScope.launch {
            val attachment = Attachment.Image(
                id = idGenerator.newId(),
                filePath = imageStorage.saveImage(recipeId, uri)
            )
            updateEditing { state -> state.copy(addedAttachments = listOf(attachment) + state.addedAttachments) }
        }
    }

    /** Creates the file/URI pair the camera app should write a photo into. */
    suspend fun prepareCameraCapture(): RecipeImageStorage.CaptureTarget = imageStorage.createCaptureTarget(recipeId)

    /** Call once the camera reports it wrote into [relativePath] (from [prepareCameraCapture]). */
    fun addPictureAttachment(relativePath: String) {
        val attachment = Attachment.Image(id = idGenerator.newId(), filePath = relativePath)
        updateEditing { state -> state.copy(addedAttachments = state.addedAttachments + attachment) }
    }

    /** Call once the camera reports it wrote into [relativePath] (from [prepareCameraCapture]). */
    fun addCoverPicture(relativePath: String) {
        val attachment = Attachment.Image(id = idGenerator.newId(), filePath = relativePath)
        updateEditing { state -> state.copy(addedAttachments = listOf(attachment) + state.addedAttachments) }
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
            recipeRepository.saveRecipe(state.toDraft())
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
        ingredients = ingredientEntriesFrom(ingredients) { idGenerator.newId() },
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
