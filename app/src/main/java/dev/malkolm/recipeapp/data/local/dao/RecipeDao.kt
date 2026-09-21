package dev.malkolm.recipeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.malkolm.recipeapp.data.local.entity.AttachmentEntity
import dev.malkolm.recipeapp.data.local.entity.RecipeEntity
import dev.malkolm.recipeapp.data.local.entity.RecipeTagCrossRef
import dev.malkolm.recipeapp.data.local.entity.TagEntity
import dev.malkolm.recipeapp.data.local.relation.RecipeSummaryRow
import dev.malkolm.recipeapp.data.local.relation.RecipeWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * SQL access for recipes. It is an abstract class (not an interface) so [saveRecipe] can be a
 * `@Transaction`: several writes that either all happen or none do, so a crash halfway through
 * a save can never leave a recipe with half its attachments.
 */
@Dao
abstract class RecipeDao {
    // Thumbnail = first attachment (by position) that has a picture: an IMAGE's own file, or the
    // saved thumbnail of a LINK/PDF. It is computed here so the list does not load whole recipes.
    @Query(
        """
        SELECT r.id AS id, r.title AS title, r.servings AS servings,
               r.cookingTimeMinutes AS cookingTimeMinutes, r.rating AS rating,
               r.updatedAt AS updatedAt,
               (SELECT CASE WHEN a.type = 'IMAGE' THEN a.filePath ELSE a.thumbnailPath END
                  FROM attachments a
                 WHERE a.recipeId = r.id
                   AND ((a.type = 'IMAGE' AND a.filePath IS NOT NULL) OR a.thumbnailPath IS NOT NULL)
                 ORDER BY a.position
                 LIMIT 1) AS thumbnailPath
          FROM recipes r
         WHERE r.deletedAt IS NULL
         ORDER BY r.updatedAt DESC
        """
    )
    abstract fun observeSummaries(): Flow<List<RecipeSummaryRow>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id AND deletedAt IS NULL")
    abstract fun observeDetails(id: String): Flow<RecipeWithDetails?>

    @Query("SELECT createdAt FROM recipes WHERE id = :id")
    protected abstract suspend fun getCreatedAt(id: String): Long?

    // @Upsert updates an existing row in place. (REPLACE would delete and re-insert it, and the
    // delete would cascade away the recipe's attachments.)
    @Upsert
    protected abstract suspend fun upsertRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM attachments WHERE recipeId = :recipeId")
    protected abstract suspend fun deleteAttachments(recipeId: String)

    @Insert
    protected abstract suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM recipe_tags WHERE recipeId = :recipeId")
    protected abstract suspend fun deleteTagLinks(recipeId: String)

    // IGNORE: if a tag with the same normalizedName already exists the new one is skipped, and
    // the existing tag is looked up afterwards by getTagsByKeys.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagsIgnoringDuplicates(tags: List<TagEntity>)

    @Query("SELECT * FROM tags WHERE normalizedName IN (:keys)")
    protected abstract suspend fun getTagsByKeys(keys: List<String>): List<TagEntity>

    @Insert
    protected abstract suspend fun insertTagLinks(links: List<RecipeTagCrossRef>)

    @Query("UPDATE recipes SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    abstract suspend fun markDeleted(id: String, at: Long)

    @Query("UPDATE recipes SET deletedAt = NULL, updatedAt = :at WHERE id = :id AND deletedAt IS NOT NULL")
    abstract suspend fun markRestored(id: String, at: Long)

    /**
     * Saves a whole recipe (row, attachments, tags) atomically. If the recipe already exists its
     * original `createdAt` is kept; [recipe]'s `createdAt` is only used for a brand-new recipe.
     */
    @Transaction
    open suspend fun saveRecipe(recipe: RecipeEntity, attachments: List<AttachmentEntity>, tags: List<TagEntity>) {
        val createdAt = getCreatedAt(recipe.id) ?: recipe.createdAt
        upsertRecipe(recipe.copy(createdAt = createdAt))

        deleteAttachments(recipe.id)
        if (attachments.isNotEmpty()) insertAttachments(attachments)

        deleteTagLinks(recipe.id)
        if (tags.isNotEmpty()) {
            insertTagsIgnoringDuplicates(tags)
            val keys = tags.map { it.normalizedName }.distinct()
            insertTagLinks(getTagsByKeys(keys).map { RecipeTagCrossRef(recipeId = recipe.id, tagId = it.id) })
        }
    }
}
