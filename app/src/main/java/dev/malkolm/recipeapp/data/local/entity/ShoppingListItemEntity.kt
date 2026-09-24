package dev.malkolm.recipeapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row in the `shopping_list_items` table. Stands alone: not linked to the recipe(s) it came from. */
@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: String?,
    val isChecked: Boolean,
    val createdAt: Long
)
