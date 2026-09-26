package dev.malkolm.recipeapp.data.calendar

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.data.local.RecipeDatabase
import dev.malkolm.recipeapp.data.repository.RoomMealPlanRepository
import dev.malkolm.recipeapp.data.repository.RoomRecipeRepository
import dev.malkolm.recipeapp.domain.model.Feature
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.repository.SelectedCalendar
import dev.malkolm.recipeapp.testutil.FakeCalendarGateway
import dev.malkolm.recipeapp.testutil.FakeFeatureSettingsRepository
import dev.malkolm.recipeapp.testutil.FakePlannerSettingsRepository
import dev.malkolm.recipeapp.testutil.MutableClock
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The sync against a real (in-memory) database and a fake phone calendar. */
@RunWith(AndroidJUnit4::class)
class MealPlanCalendarSyncTest {
    private val personal = SelectedCalendar(id = 1, displayName = "Personal")
    private val work = SelectedCalendar(id = 2, displayName = "Work")

    private lateinit var database: RecipeDatabase
    private lateinit var clock: MutableClock
    private lateinit var today: LocalDate
    private val settings = FakePlannerSettingsRepository()
    private val features = FakeFeatureSettingsRepository()
    private val calendar = FakeCalendarGateway()
    private lateinit var plan: RoomMealPlanRepository
    private lateinit var recipes: RoomRecipeRepository
    private lateinit var sync: MealPlanCalendarSync

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RecipeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        // Midday, so "today" is the same date in any time zone the tests run in.
        clock = MutableClock(Instant.parse("2026-09-25T12:00:00Z"))
        today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
        plan = RoomMealPlanRepository(database.planDao(), clock, SequentialIdGenerator("plan"))
        recipes = RoomRecipeRepository(database.recipeDao(), clock)
        sync =
            MealPlanCalendarSync(
                ApplicationProvider.getApplicationContext(),
                database.planDao(),
                settings,
                features,
                calendar,
                clock
            )
        settings.setCalendar(personal)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun recipe(id: String, title: String, ingredients: String = "") =
        recipes.saveRecipe(RecipeDraft(id = id, title = title, ingredients = ingredients))

    @Test
    fun `a planned dinner becomes a one-hour event at the dinner time, with the ingredients`() = runTest {
        recipe("r1", "Pancakes", "Batter:\nFlour (300 g)\nSalt")
        plan.setMeal(today.plusDays(1), MealType.DINNER, "r1")

        assertTrue(sync.sync().isSuccess)

        val event = calendar.eventsIn(personal.id).single()
        assertEquals("Dinner: Pancakes", event.title)
        assertEquals(today.plusDays(1).atTime(18, 0).atZone(ZoneId.systemDefault()), event.start)
        assertEquals(Duration.ofHours(1), Duration.between(event.start, event.end))
        assertEquals("Batter:\n• Flour (300 g)\n• Salt", event.description)
    }

    @Test
    fun `a grocery day becomes an event at the grocery time`() = runTest {
        settings.setGroceryTime(LocalTime.of(16, 30))
        plan.setGroceryDay(today, true)

        sync.sync()

        val event = calendar.eventsIn(personal.id).single()
        assertEquals("Grocery shopping", event.title)
        assertEquals(today.atTime(16, 30).atZone(ZoneId.systemDefault()), event.start)
    }

    @Test
    fun `changing the recipe updates the same event, removing it deletes the event`() = runTest {
        recipe("r1", "Pancakes")
        recipe("r2", "Waffles")
        plan.setMeal(today, MealType.DINNER, "r1")
        sync.sync()
        val eventId = calendar.events.keys.single()

        plan.setMeal(today, MealType.DINNER, "r2")
        sync.sync()
        assertEquals(setOf(eventId), calendar.events.keys)
        assertEquals("Dinner: Waffles", calendar.events.getValue(eventId).event.title)

        plan.removeMeal(today, MealType.DINNER)
        sync.sync()
        assertTrue(calendar.events.isEmpty())
    }

    @Test
    fun `changing a meal time moves its events`() = runTest {
        recipe("r1", "Pancakes")
        plan.setMeal(today, MealType.DINNER, "r1")
        sync.sync()

        settings.setMealTime(MealType.DINNER, LocalTime.of(19, 15))
        sync.sync()

        assertEquals(today.atTime(19, 15).atZone(ZoneId.systemDefault()), calendar.eventsIn(personal.id).single().start)
    }

    @Test
    fun `meals hidden by fewer meals per day leave the calendar, and come back when shown again`() = runTest {
        recipe("r1", "Soup")
        recipe("r2", "Pancakes")
        settings.setMealsPerDay(2)
        plan.setMeal(today, MealType.LUNCH, "r1")
        plan.setMeal(today, MealType.DINNER, "r2")
        sync.sync()
        assertEquals(2, calendar.events.size)

        settings.setMealsPerDay(1)
        sync.sync()
        assertEquals(listOf("Dinner: Pancakes"), calendar.eventsIn(personal.id).map { it.title })

        settings.setMealsPerDay(2)
        sync.sync()
        assertEquals(setOf("Lunch: Soup", "Dinner: Pancakes"), calendar.eventsIn(personal.id).map { it.title }.toSet())
    }

    @Test
    fun `a deleted recipe's event is removed`() = runTest {
        recipe("r1", "Pancakes")
        plan.setMeal(today, MealType.DINNER, "r1")
        sync.sync()

        recipes.deleteRecipe("r1")
        sync.sync()

        assertTrue(calendar.events.isEmpty())
    }

    @Test
    fun `an event deleted in the calendar app is created again`() = runTest {
        recipe("r1", "Pancakes")
        plan.setMeal(today, MealType.DINNER, "r1")
        sync.sync()

        calendar.events.clear()
        sync.sync()

        assertEquals(listOf("Dinner: Pancakes"), calendar.eventsIn(personal.id).map { it.title })
    }

    @Test
    fun `switching calendars moves the events, and stopping removes them`() = runTest {
        recipe("r1", "Pancakes")
        plan.setMeal(today, MealType.DINNER, "r1")
        plan.setGroceryDay(today.plusDays(2), true)
        sync.sync()

        sync.switchCalendar(work)
        assertTrue(calendar.eventsIn(personal.id).isEmpty())
        assertEquals(2, calendar.eventsIn(work.id).size)

        sync.switchCalendar(null)
        assertTrue(calendar.events.isEmpty())
    }

    @Test
    fun `switching the meal planner off removes its events, and switching it on brings them back`() = runTest {
        recipe("r1", "Pancakes")
        plan.setMeal(today, MealType.DINNER, "r1")
        sync.sync()

        features.setSwitchedOn(Feature.MEAL_PLANNER, false)
        sync.sync()
        assertTrue(calendar.events.isEmpty())

        features.setSwitchedOn(Feature.MEAL_PLANNER, true)
        sync.sync()
        assertEquals(listOf("Dinner: Pancakes"), calendar.eventsIn(personal.id).map { it.title })
    }

    @Test
    fun `past days are left alone`() = runTest {
        recipe("r1", "Pancakes")
        plan.setMeal(today.minusDays(1), MealType.DINNER, "r1")

        sync.sync()

        assertTrue(calendar.events.isEmpty())
    }

    @Test
    fun `nothing is written while calendar sync is off`() = runTest {
        settings.setCalendar(null)
        recipe("r1", "Pancakes")
        plan.setMeal(today, MealType.DINNER, "r1")

        assertTrue(sync.sync().isSuccess)
        assertTrue(calendar.events.isEmpty())
    }
}
