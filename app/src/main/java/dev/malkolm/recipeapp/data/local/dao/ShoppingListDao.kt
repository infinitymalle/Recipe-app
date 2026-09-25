package dev.malkolm.recipeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.malkolm.recipeapp.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

/** Deleted items keep their row with `deletedAt` set (see [ShoppingListItemEntity]); every read skips them. */
@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items WHERE deletedAt IS NULL ORDER BY isChecked ASC, createdAt ASC")
    fun observeItems(): Flow<List<ShoppingListItemEntity>>

    /** Unchecked items only, used to find a match when merging in new ingredients. */
    @Query("SELECT * FROM shopping_list_items WHERE isChecked = 0 AND deletedAt IS NULL")
    suspend fun getUnchecked(): List<ShoppingListItemEntity>

    @Upsert
    suspend fun upsertAll(items: List<ShoppingListItemEntity>)

    @Query("UPDATE shopping_list_items SET isChecked = :checked, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun setChecked(id: String, checked: Boolean, at: Long)

    @Query("UPDATE shopping_list_items SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun markDeleted(id: String, at: Long)

    @Query("UPDATE shopping_list_items SET deletedAt = :at, updatedAt = :at WHERE isChecked = 1 AND deletedAt IS NULL")
    suspend fun markCheckedDeleted(at: Long)
}
