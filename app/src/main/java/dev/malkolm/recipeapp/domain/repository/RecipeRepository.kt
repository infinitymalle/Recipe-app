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
    /** All recipes, most recently changed first. Deleted recipes are not included. */
    fun observeRecipeSummaries(): Flow<List<RecipeSummary>>

    /** One recipe with its tags and attachments, or `null` if it does not exist or was deleted. */
    fun observeRecipe(id: String): Flow<Recipe?>

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
