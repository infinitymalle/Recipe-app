package dev.malkolm.recipeapp.ui.shoppinglist

import dev.malkolm.recipeapp.domain.model.ShoppingListEntry
import dev.malkolm.recipeapp.ui.recipeedit.IngredientListItem
import dev.malkolm.recipeapp.ui.recipeedit.ingredientItemsFrom

/** A recipe's ingredient text as shopping list entries: the ingredients only, section headings skipped. */
fun shoppingEntriesFrom(ingredients: String): List<ShoppingListEntry> = ingredientItemsFrom(ingredients) { "" }
    .filterIsInstance<IngredientListItem.Entry>()
    .map { ShoppingListEntry(name = it.name, amount = it.amount) }
