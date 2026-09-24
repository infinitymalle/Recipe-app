package dev.malkolm.recipeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.malkolm.recipeapp.data.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items ORDER BY isChecked ASC, createdAt ASC")
    fun observeItems(): Flow<List<ShoppingListItemEntity>>

    /** Unchecked items only, used to find a match when merging in new ingredients. */
    @Query("SELECT * FROM shopping_list_items WHERE isChecked = 0")
    suspend fun getUnchecked(): List<ShoppingListItemEntity>

    @Upsert
    suspend fun upsertAll(items: List<ShoppingListItemEntity>)

    @Query("UPDATE shopping_list_items SET isChecked = :checked WHERE id = :id")
    suspend fun setChecked(id: String, checked: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM shopping_list_items WHERE isChecked = 1")
    suspend fun clearChecked()
}
