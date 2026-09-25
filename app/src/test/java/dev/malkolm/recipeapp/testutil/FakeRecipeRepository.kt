package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/** In-memory [RecipeRepository] for ViewModel tests: no Room, no Robolectric. */
class FakeRecipeRepository(private val clock: Clock = Clock.systemUTC()) : RecipeRepository {
    private val recipes = MutableStateFlow<Map<String, Recipe>>(emptyMap())
    private val deletedIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observeRecipeSummaries(searchQuery: String, tagId: String?): Flow<List<RecipeSummary>> =
        combine(recipes, deletedIds) { all, deleted ->
            all.values
                .filterNot { it.id in deleted }
                .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
                .filter { tagId == null || it.tags.any { tag -> tag.id == tagId } }
                .sortedByDescending { it.updatedAt }
                .map { it.toSummary() }
        }

    override fun observeRecipe(id: String): Flow<Recipe?> =
        combine(recipes, deletedIds) { all, deleted -> all[id]?.takeUnless { id in deleted } }

    override suspend fun getAllRecipes(): List<Recipe> = recipes.value.values.filterNot { it.id in deletedIds.value }

    override suspend fun saveRecipe(draft: RecipeDraft) {
        val now = clock.instant()
        recipes.update { map ->
            val existing = map[draft.id]
            map +
                (
                    draft.id to
                        Recipe(
                            id = draft.id,
                            title = draft.title,
                            ingredients = draft.ingredients,
                            servings = draft.servings,
                            cookingTimeMinutes = draft.cookingTimeMinutes,
                            rating = draft.rating,
                            notes = draft.notes,
                            method = draft.method,
                            tags = draft.tags,
                            attachments = draft.attachments,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
        }
        deletedIds.update { it - draft.id }
    }

    override suspend fun deleteRecipe(id: String) {
        deletedIds.update { it + id }
    }

    override suspend fun restoreRecipe(id: String) {
        deletedIds.update { it - id }
    }

    private fun Recipe.toSummary(): RecipeSummary {
        val thumbnail =
            attachments.firstNotNullOfOrNull { attachment ->
                when (attachment) {
                    is Attachment.Image -> attachment.filePath
                    is Attachment.Link -> attachment.thumbnailPath
                    is Attachment.Pdf -> attachment.thumbnailPath
                    is Attachment.Text -> null
                }
            }
        return RecipeSummary(
            id = id,
            title = title,
            servings = servings,
            cookingTimeMinutes = cookingTimeMinutes,
            rating = rating,
            thumbnailPath = thumbnail,
            updatedAt = updatedAt
        )
    }
}
