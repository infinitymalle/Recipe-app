package dev.malkolm.recipeapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Join table for the many-to-many relation: a recipe has many tags and a tag belongs to many
 * recipes. One row per (recipe, tag) pair; the pair is the primary key so it cannot repeat.
 */
@Entity(
    tableName = "recipe_tags",
    primaryKeys = ["recipeId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // The primary key already indexes recipeId; tagId needs its own index for "recipes with tag X".
    indices = [Index("tagId")]
)
data class RecipeTagCrossRef(val recipeId: String, val tagId: String)
