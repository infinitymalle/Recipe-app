package dev.malkolm.recipeapp.data.repository

import dev.malkolm.recipeapp.data.local.dao.RecipeDao
import dev.malkolm.recipeapp.data.local.mapper.toDomain
import dev.malkolm.recipeapp.data.local.mapper.toEntities
import dev.malkolm.recipeapp.data.local.mapper.toEntity
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [RecipeRepository] backed by the on-device Room database. Room runs its queries off the main
 * thread itself, so callers can use these functions from a ViewModel directly.
 */
class RoomRecipeRepository
@Inject
constructor(private val dao: RecipeDao, private val clock: Clock) :
    RecipeRepository {
    override fun observeRecipeSummaries(searchQuery: String, tagId: String?): Flow<List<RecipeSummary>> =
        dao.observeSummaries(searchQuery.trim(), tagId).map { rows -> rows.map { it.toDomain() } }

    override fun observeRecipe(id: String): Flow<Recipe?> = dao.observeDetails(id).map { it?.toDomain() }

    override suspend fun getAllRecipes(): List<Recipe> = dao.getAllDetails().map { it.toDomain() }

    override suspend fun saveRecipe(draft: RecipeDraft) {
        dao.saveRecipe(
            recipe = draft.toEntity(now = clock.instant()),
            attachments = draft.attachments.toEntities(draft.id),
            tags = draft.tags.map { it.toEntity() }
        )
    }

    override suspend fun deleteRecipe(id: String) = dao.markDeleted(id, clock.millis())

    override suspend fun restoreRecipe(id: String) = dao.markRestored(id, clock.millis())
}
