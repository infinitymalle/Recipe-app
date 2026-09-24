package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import kotlinx.coroutines.flow.Flow

/**
 * The only way the rest of the app reads and writes recipes.
 *
 * ViewModels depend on this interface, never on Room. The current implementation stores
 * everything on the phone; a future one can add cloud sync behind the same interface, so the
 * screens do not need to change.
 *
 * `observe*` functions return a [Flow] that emits again whenever the data changes, so the UI
 * stays up to date without manual refreshing.
 */
interface RecipeRepository {
    /**
     * Recipes, most recently changed first. Deleted recipes are not included.
     *
     * [searchQuery] keeps only recipes whose title contains it (case-insensitive for ASCII;
     * blank means no filter). [tagId] keeps only recipes carrying that tag (`null` means no
     * filter). Both can be set at once.
     */
    fun observeRecipeSummaries(searchQuery: String = "", tagId: String? = null): Flow<List<RecipeSummary>>

    /** One recipe with its tags and attachments, or `null` if it does not exist or was deleted. */
    fun observeRecipe(id: String): Flow<Recipe?>

    /** Every recipe with its tags and attachments, for a full export. Deleted recipes are not included. */
    suspend fun getAllRecipes(): List<Recipe>

    /**
     * Creates the recipe, or updates it if [RecipeDraft.id] already exists. Tags are matched by
     * name ignoring case, so existing tags are reused. Attachments and tags are replaced by the
     * ones in [draft].
     */
    suspend fun saveRecipe(draft: RecipeDraft)

    /**
     * Deletes the recipe from view. The data is kept so the delete can be undone with
     * [restoreRecipe] (and so a future sync can tell other devices about the delete).
     */
    suspend fun deleteRecipe(id: String)

    /** Undoes [deleteRecipe]. Does nothing if the recipe does not exist. */
    suspend fun restoreRecipe(id: String)
}
