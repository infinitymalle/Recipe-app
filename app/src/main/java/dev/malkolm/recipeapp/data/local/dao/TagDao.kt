package dev.malkolm.recipeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import dev.malkolm.recipeapp.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    // A tag that no live recipe uses any more stays in the table (harmless) but is hidden here,
    // so a filter never offers a tag that would match nothing.
    @Query(
        """
        SELECT * FROM tags t
         WHERE EXISTS (SELECT 1
                         FROM recipe_tags rt
                         JOIN recipes r ON r.id = rt.recipeId
                        WHERE rt.tagId = t.id AND r.deletedAt IS NULL)
         ORDER BY t.normalizedName
        """
    )
    fun observeTagsInUse(): Flow<List<TagEntity>>
}
