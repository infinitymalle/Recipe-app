package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.ShoppingListEntry
import dev.malkolm.recipeapp.domain.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

/** The only way the rest of the app reads and writes the shopping list. */
interface ShoppingListRepository {
    /** Unchecked items first, each group oldest-added first. */
    fun observeItems(): Flow<List<ShoppingListItem>>

    /** Adds one manually-typed item. Does nothing if [name] is blank. */
    suspend fun addItem(name: String, amount: String?)

    /** Merges ingredients (e.g. from a recipe) into the list; see [dev.malkolm.recipeapp.domain.model.mergeShoppingEntries]. */
    suspend fun addEntries(entries: List<ShoppingListEntry>)

    suspend fun setChecked(id: String, checked: Boolean)

    suspend fun deleteItem(id: String)

    /** Removes every checked-off item, e.g. after a shopping trip. */
    suspend fun clearChecked()

    /** Removes every item, checked or not. */
    suspend fun clearAll()
}
