package dev.malkolm.recipeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row in the `shopping_list_items` table. Stands alone: not linked to the recipe(s) it came from.
 *
 * Timestamps are epoch milliseconds. Like recipes, items are ready for a future sync between
 * phones: [updatedAt] changes on every edit (so the newer change can win) and deleting only sets
 * [deletedAt] (so the delete itself can be sent to other phones) instead of removing the row.
 */
@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: String?,
    val isChecked: Boolean,
    val createdAt: Long,
    // The default only matters for rows that existed before this column (see MIGRATION_2_3).
    @ColumnInfo(defaultValue = "0") val updatedAt: Long,
    val deletedAt: Long? = null
)
