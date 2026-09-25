package dev.malkolm.recipeapp.ui.recipeedit

/**
 * One row of the "Recipe" section the edit screen builds through its "Add ingredient"/"Add
 * title" dialogs and buttons, and that can be reordered to group ingredients under a heading
 * (e.g. "Mashed potatoes", "Meat", "Salad" as separate sections within one recipe).
 */
sealed interface IngredientListItem {
    val id: String

    /** A section title, e.g. "Mashed potatoes". */
    data class Heading(override val id: String, val text: String) : IngredientListItem

    data class Entry(override val id: String, val name: String, val amount: String? = null) : IngredientListItem
}

// The database still stores this as one string per recipe (see docs/database.md), so the list
// built here is encoded as one line per item - "Flour (2 cups)" for an entry with an amount,
// "Flour" for one without, "Mashed potatoes:" for a heading - and decoded back the same way when
// an existing recipe is opened to edit. This also keeps the plain-text column readable as-is on
// the detail screen (a trailing colon reads as a section title there too), with no schema change.
//
// Brackets are allowed in names and amounts: the amount is always the LAST bracket group on the
// line (it may itself contain one level of brackets, "Flour (2 cups (250 g))"). An entry without
// an amount whose name would otherwise be misread - it ends in ")" like "Butter (softened)", or in
// ":" like a heading - gets an empty "()" appended, which decodes back to "no amount".
private val amountSuffix = Regex("""^(.*) \(((?:[^()]|\([^()]*\))*)\)$""")

fun List<IngredientListItem>.toIngredientsText(): String = joinToString("\n") { item ->
    when (item) {
        is IngredientListItem.Heading -> "${item.text}:"
        is IngredientListItem.Entry -> item.toLine()
    }
}

private fun IngredientListItem.Entry.toLine(): String = when {
    !amount.isNullOrBlank() -> "$name ($amount)"
    name.endsWith(")") || name.endsWith(":") -> "$name ()"
    else -> name
}

fun ingredientItemsFrom(text: String, nextId: () -> String): List<IngredientListItem> = text
    .lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { line ->
        when {
            line.endsWith(":") ->
                IngredientListItem.Heading(id = nextId(), text = line.removeSuffix(":").trim())

            else -> {
                val match = amountSuffix.matchEntire(line)
                if (match != null) {
                    val amount = match.groupValues[2].trim().ifEmpty { null }
                    IngredientListItem.Entry(id = nextId(), name = match.groupValues[1], amount = amount)
                } else {
                    IngredientListItem.Entry(id = nextId(), name = line)
                }
            }
        }
    }

/** Swaps the item at [id] with its neighbor in [direction]; a no-op past either end of the list. */
fun List<IngredientListItem>.moved(id: String, direction: Int): List<IngredientListItem> {
    val index = indexOfFirst { it.id == id }
    val targetIndex = index + direction
    if (index < 0 || targetIndex < 0 || targetIndex >= size) return this
    return toMutableList().apply { this[index] = this[targetIndex].also { this[targetIndex] = this[index] } }
}
