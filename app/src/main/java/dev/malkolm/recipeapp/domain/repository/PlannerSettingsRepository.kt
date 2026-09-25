package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.MealType
import java.time.LocalTime
import kotlinx.coroutines.flow.StateFlow

/** The phone calendar the meal plan is copied into. */
data class SelectedCalendar(val id: Long, val displayName: String)

/** The meal planner's settings, persisted across app restarts. */
interface PlannerSettingsRepository {
    /** How many weeks ahead the planner shows, within [WEEKS_AHEAD_RANGE]. */
    val weeksAhead: StateFlow<Int>

    fun setWeeksAhead(weeks: Int)

    /** How many meals a day can be planned, see [MealType.activeFor]. Defaults to 1 (dinner). */
    val mealsPerDay: StateFlow<Int>

    fun setMealsPerDay(count: Int)

    /** When each meal's calendar event starts. */
    val mealTimes: StateFlow<Map<MealType, LocalTime>>

    fun setMealTime(mealType: MealType, time: LocalTime)

    /** When a grocery day's calendar event starts. */
    val groceryTime: StateFlow<LocalTime>

    fun setGroceryTime(time: LocalTime)

    /** `null` while calendar sync is off. */
    val calendar: StateFlow<SelectedCalendar?>

    fun setCalendar(calendar: SelectedCalendar?)

    companion object {
        val WEEKS_AHEAD_RANGE = 1..8
        const val DEFAULT_WEEKS_AHEAD = 2
        const val DEFAULT_MEALS_PER_DAY = 1
        val DEFAULT_MEAL_TIMES =
            mapOf(
                MealType.BREAKFAST to LocalTime.of(8, 0),
                MealType.LUNCH to LocalTime.of(12, 0),
                MealType.DINNER to LocalTime.of(18, 0)
            )
        val DEFAULT_GROCERY_TIME: LocalTime = LocalTime.of(17, 0)
    }
}
