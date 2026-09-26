package dev.malkolm.recipeapp.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.data.RecipeImageStorage
import dev.malkolm.recipeapp.data.calendar.MealPlanCalendarSync
import dev.malkolm.recipeapp.data.local.RecipeDatabase
import dev.malkolm.recipeapp.data.repository.RoomMealPlanRepository
import dev.malkolm.recipeapp.data.repository.RoomRecipeRepository
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.PlanEntry
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.SelectedCalendar
import dev.malkolm.recipeapp.testutil.FakeCalendarGateway
import dev.malkolm.recipeapp.testutil.FakeFeatureSettingsRepository
import dev.malkolm.recipeapp.testutil.FakePlannerSettingsRepository
import dev.malkolm.recipeapp.testutil.FakeThemeSettingsRepository
import dev.malkolm.recipeapp.testutil.MutableClock
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** A full backup's meal plan and settings, exported from one "phone" and restored on another. */
@RunWith(AndroidJUnit4::class)
class AppBackupExtrasTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = MutableClock(Instant.parse("2026-09-25T12:00:00Z"))
    private val day = LocalDate.of(2026, 9, 28)
    private val phones = mutableListOf<Phone>()

    /** One phone's database, settings and backup service. */
    private inner class Phone(idPrefix: String = "plan") {
        val database: RecipeDatabase =
            Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).allowMainThreadQueries().build()
        val recipes = RoomRecipeRepository(database.recipeDao(), clock)
        val plan = RoomMealPlanRepository(database.planDao(), clock, SequentialIdGenerator(idPrefix))
        val theme = FakeThemeSettingsRepository()
        val planner = FakePlannerSettingsRepository()
        val calendar = FakeCalendarGateway()
        val backup =
            RecipeBackupService(
                context,
                recipes,
                SequentialIdGenerator(),
                AppBackupExtras(
                    database.planDao(),
                    theme,
                    planner,
                    RecipeImageStorage(context),
                    MealPlanCalendarSync(
                        context,
                        database.planDao(),
                        planner,
                        FakeFeatureSettingsRepository(),
                        calendar,
                        clock
                    ),
                    clock
                )
            )

        init {
            phones += this
        }
    }

    @After
    fun tearDown() = phones.forEach { it.database.close() }

    @Test
    fun `the meal plan and settings survive a backup and restore`() = runTest {
        val old = Phone()
        old.recipes.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        old.plan.setMeal(day, MealType.DINNER, "r1")
        old.plan.setGroceryDay(day, true)
        old.theme.setThemeMode(ThemeMode.DARK)
        old.theme.setBackgroundBlur(14)
        old.planner.setWeeksAhead(4)
        old.planner.setMealsPerDay(2)
        old.planner.setMealTime(MealType.DINNER, LocalTime.of(19, 30))
        old.planner.setGroceryTime(LocalTime.of(10, 0))
        val zip = Uri.fromFile(File(context.cacheDir, "full-backup.zip"))
        old.backup.export(zip)

        val new = Phone("newplan")
        new.backup.import(zip)

        val restored = new.plan.observePlan(day, day).first()
        assertEquals("Pancakes", restored.filterIsInstance<PlanEntry.Meal>().single().recipeTitle)
        assertEquals(1, restored.count { it is PlanEntry.GroceryTrip })
        assertEquals(ThemeMode.DARK, new.theme.themeMode.value)
        assertEquals(14, new.theme.backgroundBlur.value)
        assertEquals(4, new.planner.weeksAhead.value)
        assertEquals(2, new.planner.mealsPerDay.value)
        assertEquals(LocalTime.of(19, 30), new.planner.mealTimes.value[MealType.DINNER])
        assertEquals(LocalTime.of(10, 0), new.planner.groceryTime.value)
    }

    @Test
    fun `restoring onto a phone with a calendar puts the restored plan in it`() = runTest {
        val old = Phone()
        old.recipes.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        old.plan.setMeal(clock.instant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(), MealType.DINNER, "r1")
        val zip = Uri.fromFile(File(context.cacheDir, "full-backup-calendar.zip"))
        old.backup.export(zip)

        val new = Phone("newplan")
        new.planner.setCalendar(SelectedCalendar(1, "Personal"))
        new.backup.import(zip)

        assertEquals(listOf("Dinner: Pancakes"), new.calendar.eventsIn(1).map { it.title })
    }

    @Test
    fun `a restored meal replaces what this phone had planned in the same slot`() = runTest {
        val old = Phone()
        old.recipes.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes"))
        old.plan.setMeal(day, MealType.DINNER, "r1")
        val zip = Uri.fromFile(File(context.cacheDir, "full-backup-slot.zip"))
        old.backup.export(zip)

        val new = Phone("newplan")
        new.recipes.saveRecipe(RecipeDraft(id = "r2", title = "Waffles"))
        new.plan.setMeal(day, MealType.DINNER, "r2")
        new.backup.import(zip)

        val meals = new.plan.observePlan(day, day).first().filterIsInstance<PlanEntry.Meal>()
        assertEquals(listOf("Pancakes"), meals.map { it.recipeTitle })
    }

    @Test
    fun `entries and settings that are not understood are skipped`() = runTest {
        val zipFile = File(context.cacheDir, "odd-backup.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(
                (
                    """{"version":2,"recipes":[],"mealPlan":[""" +
                        """{"id":"p1","date":"not a date","kind":"MEAL","mealType":"DINNER","recipeId":"r1"},""" +
                        """{"id":"p2","date":"2026-09-28","kind":"MEAL","mealType":"SUPPER","recipeId":"r1"},""" +
                        """{"id":"../p3","date":"2026-09-28","kind":"GROCERY"},""" +
                        """{"id":"p4","date":"2026-09-28","kind":"GROCERY"}],""" +
                        """"settings":{"themeMode":"NEON","shoppingListImagePath":"../databases/recipes.db",""" +
                        """"mealTimes":{"DINNER":"25:99"}}}"""
                    ).toByteArray()
            )
            zip.closeEntry()
        }
        val phone = Phone()

        phone.backup.import(Uri.fromFile(zipFile))

        assertEquals(listOf("p4"), phone.plan.observePlan(day, day).first().map { it.id })
        assertEquals(ThemeMode.SYSTEM, phone.theme.themeMode.value)
        assertNull(phone.theme.shoppingListImagePath.value)
        assertEquals(LocalTime.of(18, 0), phone.planner.mealTimes.value[MealType.DINNER])
    }
}
