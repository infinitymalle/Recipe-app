package dev.malkolm.recipeapp.domain.model

import java.time.Instant

data class ShoppingListItem(
    val id: String,
    val name: String,
    val amount: String?,
    val isChecked: Boolean,
    val addedAt: Instant
)

/** One ingredient to merge into the shopping list, e.g. from a recipe's ingredient list. */
data class ShoppingListEntry(val name: String, val amount: String?)

/**
 * Merges [newEntries] into [existing] (which should only contain unchecked items: a checked-off
 * item means "already bought", so a new entry with the same name should start a fresh line rather
 * than silently reappearing as still-checked).
 *
 * Returns only the items that changed or were added, i.e. what the caller should write. A name
 * already present with a mergeable amount gets its quantity combined (see [combineAmounts]) under
 * its existing id; anything else becomes a new item.
 */
fun mergeShoppingEntries(
    existing: List<ShoppingListItem>,
    newEntries: List<ShoppingListEntry>,
    newId: () -> String,
    now: Instant
): List<ShoppingListItem> {
    val byNormalizedName = existing.associateBy { it.name.trim().lowercase() }.toMutableMap()
    val changed = mutableListOf<ShoppingListItem>()

    for (entry in newEntries) {
        val name = entry.name.trim()
        if (name.isEmpty()) continue
        val amount = entry.amount?.trim()?.takeIf { it.isNotEmpty() }
        val key = name.lowercase()
        val current = byNormalizedName[key]

        if (current == null) {
            val item = ShoppingListItem(id = newId(), name = name, amount = amount, isChecked = false, addedAt = now)
            byNormalizedName[key] = item
            changed += item
        } else {
            val combined = combineAmounts(current.amount, amount)
            if (combined != current.amount) {
                val updated = current.copy(amount = combined)
                byNormalizedName[key] = updated
                changed += updated
            }
        }
    }

    return changed
}

private val leadingNumber = Regex("""^\s*(\d+(?:\.\d+)?)\s*(.*)$""")

/**
 * Combines two ingredient amounts for the same item. Same unit ("2 cups" + "1 cups") sums to
 * "3 cups"; anything not parseable the same way is kept as both, joined with " + ", so no
 * quantity information is ever silently dropped.
 */
fun combineAmounts(a: String?, b: String?): String? {
    val cleanA = a?.trim()?.takeIf { it.isNotEmpty() }
    val cleanB = b?.trim()?.takeIf { it.isNotEmpty() }
    if (cleanA == null) return cleanB
    if (cleanB == null) return cleanA

    val matchA = leadingNumber.matchEntire(cleanA)
    val matchB = leadingNumber.matchEntire(cleanB)
    if (matchA != null && matchB != null) {
        val unitA = matchA.groupValues[2].trim()
        val unitB = matchB.groupValues[2].trim()
        if (unitA.equals(unitB, ignoreCase = true)) {
            val sum = matchA.groupValues[1].toDouble() + matchB.groupValues[1].toDouble()
            val formattedSum = if (sum == sum.toLong().toDouble()) sum.toLong().toString() else sum.toString()
            return if (unitA.isEmpty()) formattedSum else "$formattedSum $unitA"
        }
    }
    return "$cleanA + $cleanB"
}
