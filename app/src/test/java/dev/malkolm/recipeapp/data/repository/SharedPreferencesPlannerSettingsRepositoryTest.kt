package dev.malkolm.recipeapp.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.SelectedCalendar
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesPlannerSettingsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun reloaded() = SharedPreferencesPlannerSettingsRepository(context)

    @Test
    fun `defaults are two weeks, dinner only, and no calendar`() {
        val settings = reloaded()

        assertEquals(PlannerSettingsRepository.DEFAULT_WEEKS_AHEAD, settings.weeksAhead.value)
        assertEquals(1, settings.mealsPerDay.value)
        assertEquals(LocalTime.of(18, 0), settings.mealTimes.value[MealType.DINNER])
        assertNull(settings.calendar.value)
    }

    @Test
    fun `settings are kept within range and read back by a new instance`() {
        val settings = reloaded()

        settings.setWeeksAhead(50)
        settings.setMealsPerDay(3)
        settings.setMealTime(MealType.LUNCH, LocalTime.of(11, 45))
        settings.setGroceryTime(LocalTime.of(9, 5))
        settings.setCalendar(SelectedCalendar(7, "Personal"))

        val again = reloaded()
        assertEquals(PlannerSettingsRepository.WEEKS_AHEAD_RANGE.last, again.weeksAhead.value)
        assertEquals(3, again.mealsPerDay.value)
        assertEquals(LocalTime.of(11, 45), again.mealTimes.value[MealType.LUNCH])
        assertEquals(LocalTime.of(9, 5), again.groceryTime.value)
        assertEquals(SelectedCalendar(7, "Personal"), again.calendar.value)

        settings.setCalendar(null)
        assertNull(reloaded().calendar.value)
    }
}
