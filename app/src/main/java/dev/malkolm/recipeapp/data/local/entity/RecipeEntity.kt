package dev.malkolm.recipeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row in the `recipes` table.
 *
 * Timestamps are epoch milliseconds. [deletedAt] is `null` for a live recipe and the time of
 * deletion otherwise ("soft delete"): the row stays so a delete can be undone and, later, synced.
 */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val ingredients: String,
    // The default only matters for rows that existed before this column (see MIGRATION_3_4).
    @ColumnInfo(defaultValue = "") val method: String,
    val servings: Int?,
    val cookingTimeMinutes: Int?,
    val rating: Int?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
