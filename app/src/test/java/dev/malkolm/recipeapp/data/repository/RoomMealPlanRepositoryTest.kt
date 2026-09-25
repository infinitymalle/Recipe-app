package dev.malkolm.recipeapp.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.data.local.RecipeDatabase
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.PlanEntry
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.testutil.MutableClock
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMealPlanRepositoryTest {
    private val day = LocalDate.of(2026, 9, 28)

    private lateinit var database: RecipeDatabase
    private lateinit var plan: RoomMealPlanRepository
    private lateinit var recipes: RoomRecipeRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RecipeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val clock = MutableClock(Instant.parse("2026-09-25T12:00:00Z"))
        plan = RoomMealPlanRepository(database.planDao(), clock, SequentialIdGenerator("plan"))
        recipes = RoomRecipeRepository(database.recipeDao(), clock)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun planOn(date: LocalDate) = plan.observePlan(date, date).first()

    @Test
    fun `a planned meal shows with its recipe's title, and planning again replaces it`() = runTest {
        recipes.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        recipes.saveRecipe(RecipeDraft(id = "r2", title = "Waffles"))

        plan.setMeal(day, MealType.DINNER, "r1")
        plan.setMeal(day, MealType.DINNER, "r2")

        val meal = planOn(day).single() as PlanEntry.Meal
        assertEquals(MealType.DINNER, meal.mealType)
        assertEquals("Waffles", meal.recipeTitle)
    }

    @Test
    fun `different meals on the same day are kept apart, and removing one keeps the other`() = runTest {
        recipes.saveRecipe(RecipeDraft(id = "r1", title = "Soup"))
        plan.setMeal(day, MealType.LUNCH, "r1")
        plan.setMeal(day, MealType.DINNER, "r1")

        plan.removeMeal(day, MealType.LUNCH)

        assertEquals(listOf(MealType.DINNER), planOn(day).map { (it as PlanEntry.Meal).mealType })
    }

    @Test
    fun `grocery days are toggled on and off`() = runTest {
        plan.setGroceryDay(day, true)
        plan.setGroceryDay(day, true)
        assertEquals(1, planOn(day).count { it is PlanEntry.GroceryTrip })

        plan.setGroceryDay(day, false)
        assertTrue(planOn(day).isEmpty())
    }

    @Test
    fun `a deleted recipe stays planned but without a title`() = runTest {
        recipes.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        plan.setMeal(day, MealType.DINNER, "r1")

        recipes.deleteRecipe("r1")

        assertNull((planOn(day).single() as PlanEntry.Meal).recipeTitle)
    }

    @Test
    fun `only the requested days are returned`() = runTest {
        plan.setGroceryDay(day.minusDays(1), true)
        plan.setGroceryDay(day, true)
        plan.setGroceryDay(day.plusDays(7), true)

        assertEquals(listOf(day, day.plusDays(7)), plan.observePlan(day, day.plusDays(7)).first().map { it.date })
    }
}
