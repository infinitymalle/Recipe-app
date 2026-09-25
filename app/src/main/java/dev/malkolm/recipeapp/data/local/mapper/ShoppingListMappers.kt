package dev.malkolm.recipeapp.data.local.mapper

import dev.malkolm.recipeapp.data.local.entity.ShoppingListItemEntity
import dev.malkolm.recipeapp.domain.model.ShoppingListItem
import java.time.Instant

fun ShoppingListItemEntity.toDomain() = ShoppingListItem(
    id = id,
    name = name,
    amount = amount,
    isChecked = isChecked,
    addedAt = Instant.ofEpochMilli(createdAt)
)

/** A live (not deleted) row for this item, last changed at [updatedAt]. */
fun ShoppingListItem.toEntity(updatedAt: Instant) = ShoppingListItemEntity(
    id = id,
    name = name,
    amount = amount,
    isChecked = isChecked,
    createdAt = addedAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)
