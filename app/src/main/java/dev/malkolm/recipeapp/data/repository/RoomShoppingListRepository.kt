package dev.malkolm.recipeapp.data.repository

import dev.malkolm.recipeapp.data.local.dao.ShoppingListDao
import dev.malkolm.recipeapp.data.local.mapper.toDomain
import dev.malkolm.recipeapp.data.local.mapper.toEntity
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.ShoppingListEntry
import dev.malkolm.recipeapp.domain.model.ShoppingListItem
import dev.malkolm.recipeapp.domain.model.mergeShoppingEntries
import dev.malkolm.recipeapp.domain.repository.ShoppingListRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomShoppingListRepository
@Inject
constructor(
    private val dao: ShoppingListDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator
) : ShoppingListRepository {
    override fun observeItems(): Flow<List<ShoppingListItem>> =
        dao.observeItems().map { rows -> rows.map { it.toDomain() } }

    override suspend fun addItem(name: String, amount: String?) = addEntries(listOf(ShoppingListEntry(name, amount)))

    override suspend fun addEntries(entries: List<ShoppingListEntry>) {
        val existing = dao.getUnchecked().map { it.toDomain() }
        val now = clock.instant()
        val changed = mergeShoppingEntries(existing, entries, idGenerator::newId, now)
        if (changed.isNotEmpty()) dao.upsertAll(changed.map { it.toEntity(updatedAt = now) })
    }

    override suspend fun setChecked(id: String, checked: Boolean) = dao.setChecked(id, checked, clock.millis())

    override suspend fun deleteItem(id: String) = dao.markDeleted(id, clock.millis())

    override suspend fun clearChecked() = dao.markCheckedDeleted(clock.millis())
}
