package dev.malkolm.recipeapp.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import dev.malkolm.recipeapp.data.local.entity.AttachmentEntity
import dev.malkolm.recipeapp.data.local.entity.RecipeEntity
import dev.malkolm.recipeapp.data.local.entity.RecipeTagCrossRef
import dev.malkolm.recipeapp.data.local.entity.TagEntity

/** A recipe row together with its attachments and tags, loaded in one query. */
data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val attachments: List<AttachmentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy =
            Junction(
                value = RecipeTagCrossRef::class,
                parentColumn = "recipeId",
                entityColumn = "tagId"
            )
    )
    val tags: List<TagEntity>
)

/** One row of the recipe list query: only the columns the list screen shows. */
data class RecipeSummaryRow(
    val id: String,
    val title: String,
    val servings: Int?,
    val cookingTimeMinutes: Int?,
    val rating: Int?,
    val thumbnailPath: String?,
    val updatedAt: Long
)
