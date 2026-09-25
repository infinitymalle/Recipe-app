package dev.malkolm.recipeapp.ui.mealplan

import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.PlanEntry
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MealPlanDaysTest {
    private val monday = LocalDate.of(2026, 9, 28)

    @Test
    fun `every day in the range is listed, planned or not`() {
        val days = daysFrom(monday, monday.plusDays(13), emptyList(), MealType.activeFor(1))

        assertEquals(14, days.size)
        assertEquals(monday.plusDays(13), days.last().date)
        assertTrue(
            days.all { day ->
                day.meals.map { it.mealType } == listOf(MealType.DINNER) &&
                    day.meals.all { it.meal == null }
            }
        )
    }

    @Test
    fun `each day shows its active meals in order, filled from the plan, and its grocery trip`() {
        val dinner = PlanEntry.Meal("1", monday, MealType.DINNER, "r1", "Pancakes")
        val breakfast = PlanEntry.Meal("2", monday, MealType.BREAKFAST, "r2", "Porridge")
        val entries = listOf(dinner, breakfast, PlanEntry.GroceryTrip("3", monday.plusDays(1)))

        val days = daysFrom(monday, monday.plusDays(1), entries, MealType.activeFor(2))

        assertEquals(listOf(MealType.LUNCH, MealType.DINNER), days[0].meals.map { it.mealType })
        assertNull(days[0].meals[0].meal)
        assertEquals(dinner, days[0].meals[1].meal)
        assertFalse(days[0].isGroceryDay)
        assertTrue(days[1].isGroceryDay)
    }

    private fun meal(day: Long, type: MealType = MealType.DINNER, title: String? = "Recipe $day") =
        PlanEntry.Meal("m$day$type", monday.plusDays(day), type, "r$day", title)

    @Test
    fun `a grocery trip covers its own day up to the day before the next grocery day`() {
        val entries =
            listOf(
                meal(0),
                PlanEntry.GroceryTrip("g1", monday.plusDays(1)),
                meal(1),
                meal(2),
                meal(3),
                PlanEntry.GroceryTrip("g2", monday.plusDays(4)),
                meal(4)
            )

        val meals = mealsForGroceryTrip(monday.plusDays(1), entries, MealType.activeFor(1))

        assertEquals(listOf(monday.plusDays(1), monday.plusDays(2), monday.plusDays(3)), meals.map { it.date })
    }

    @Test
    fun `without a next grocery day, the trip covers the rest of the plan`() {
        val entries = listOf(PlanEntry.GroceryTrip("g1", monday), meal(0), meal(5), meal(12))

        assertEquals(3, mealsForGroceryTrip(monday, entries, MealType.activeFor(1)).size)
    }

    @Test
    fun `hidden meals and deleted recipes are not shopped for`() {
        val entries =
            listOf(
                PlanEntry.GroceryTrip("g1", monday),
                meal(0, MealType.LUNCH),
                meal(1, MealType.DINNER, title = null),
                meal(2, MealType.DINNER)
            )

        assertEquals(
            listOf(monday.plusDays(2)),
            mealsForGroceryTrip(monday, entries, MealType.activeFor(1)).map {
                it.date
            }
        )
    }

    @Test
    fun `meals per day adds lunch, then breakfast`() {
        assertEquals(listOf(MealType.DINNER), MealType.activeFor(1))
        assertEquals(listOf(MealType.LUNCH, MealType.DINNER), MealType.activeFor(2))
        assertEquals(listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER), MealType.activeFor(3))
        assertEquals(listOf(MealType.DINNER), MealType.activeFor(0))
    }
}
