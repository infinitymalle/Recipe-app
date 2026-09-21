package dev.malkolm.recipeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in `tags`. [name] is what the user sees. [normalizedName] is the lower-cased form
 * (see `TagNames`), and the unique index on it is what stops "Dessert" and "dessert" from
 * becoming two tags.
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class TagEntity(@PrimaryKey val id: String, val name: String, val normalizedName: String)
