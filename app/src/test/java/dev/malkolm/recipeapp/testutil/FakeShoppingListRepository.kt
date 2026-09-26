package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.ShoppingListEntry
import dev.malkolm.recipeapp.domain.model.ShoppingListItem
import dev.malkolm.recipeapp.domain.model.mergeShoppingEntries
import dev.malkolm.recipeapp.domain.repository.ShoppingListRepository
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [ShoppingListRepository] for ViewModel tests: no Room, no Robolectric. */
class FakeShoppingListRepository(
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: IdGenerator = SequentialIdGenerator("shopping")
) : ShoppingListRepository {
    private val items = MutableStateFlow<List<ShoppingListItem>>(emptyList())

    override fun observeItems(): Flow<List<ShoppingListItem>> = items

    override suspend fun addItem(name: String, amount: String?) = addEntries(listOf(ShoppingListEntry(name, amount)))

    override suspend fun addEntries(entries: List<ShoppingListEntry>) {
        val unchecked = items.value.filterNot { it.isChecked }
        val changed = mergeShoppingEntries(unchecked, entries, idGenerator::newId, clock.instant())
        if (changed.isEmpty()) return
        items.update { current ->
            val byId = current.associateBy { it.id }.toMutableMap()
            changed.forEach { byId[it.id] = it }
            byId.values.toList()
        }
    }

    override suspend fun setChecked(id: String, checked: Boolean) {
        items.update { list -> list.map { if (it.id == id) it.copy(isChecked = checked) else it } }
    }

    override suspend fun deleteItem(id: String) {
        items.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun clearChecked() {
        items.update { list -> list.filterNot { it.isChecked } }
    }

    override suspend fun clearAll() {
        items.value = emptyList()
    }
}
