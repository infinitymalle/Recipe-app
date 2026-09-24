package dev.malkolm.recipeapp.ui.recipeedit

/** One row of the ingredient list the edit screen builds through its "Add ingredient" dialog. */
data class IngredientEntry(val id: String, val name: String, val amount: String? = null)

// The database still stores ingredients as one string per recipe (see docs/database.md), so the
// list built here is encoded as one line per ingredient - "Flour (2 cups)", or just "Flour" when
// no amount was given - and decoded back the same way when an existing recipe is opened to edit.
// This keeps the plain-text column readable as-is on the detail screen, with no schema change.
private val amountSuffix = Regex("""^(.*) \(([^()]*)\)$""")

fun List<IngredientEntry>.toIngredientsText(): String = joinToString("\n") { entry ->
    if (entry.amount.isNullOrBlank()) entry.name else "${entry.name} (${entry.amount})"
}

fun ingredientEntriesFrom(text: String, nextId: () -> String): List<IngredientEntry> = text
    .lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { line ->
        val match = amountSuffix.matchEntire(line)
        if (match != null) {
            IngredientEntry(id = nextId(), name = match.groupValues[1], amount = match.groupValues[2])
        } else {
            IngredientEntry(id = nextId(), name = line)
        }
    }
