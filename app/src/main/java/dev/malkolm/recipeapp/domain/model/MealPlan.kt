package dev.malkolm.recipeapp.domain.model

import java.time.LocalDate

/** A meal of the day that recipes can be planned for. */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER;

    companion object {
        val MEALS_PER_DAY_RANGE = 1..3

        /**
         * The meals shown for [mealsPerDay] (a setting): 1 is dinner only, 2 adds lunch, 3 adds
         * breakfast. Plans for meals outside this stay saved, just hidden and not in the calendar.
         */
        fun activeFor(mealsPerDay: Int): List<MealType> = when (mealsPerDay.coerceIn(MEALS_PER_DAY_RANGE)) {
            1 -> listOf(DINNER)
            2 -> listOf(LUNCH, DINNER)
            else -> listOf(BREAKFAST, LUNCH, DINNER)
        }
    }
}

/** Something planned on a [date]: a recipe for one of its meals, or a grocery shopping trip. */
sealed interface PlanEntry {
    val id: String
    val date: LocalDate

    /** [recipeTitle] is `null` when the recipe has since been deleted. */
    data class Meal(
        override val id: String,
        override val date: LocalDate,
        val mealType: MealType,
        val recipeId: String,
        val recipeTitle: String?
    ) : PlanEntry

    data class GroceryTrip(override val id: String, override val date: LocalDate) : PlanEntry
}
