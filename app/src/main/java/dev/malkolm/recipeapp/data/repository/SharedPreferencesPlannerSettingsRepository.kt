package dev.malkolm.recipeapp.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository.Companion.DEFAULT_GROCERY_TIME
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository.Companion.DEFAULT_MEALS_PER_DAY
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository.Companion.DEFAULT_MEAL_TIMES
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository.Companion.DEFAULT_WEEKS_AHEAD
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository.Companion.WEEKS_AHEAD_RANGE
import dev.malkolm.recipeapp.domain.repository.SelectedCalendar
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "settings"
private const val KEY_WEEKS_AHEAD = "planner_weeks_ahead"
private const val KEY_MEALS_PER_DAY = "planner_meals_per_day"
private const val KEY_MEAL_TIME_PREFIX = "planner_time_"
private const val KEY_GROCERY_TIME = "planner_grocery_time"
private const val KEY_CALENDAR_ID = "planner_calendar_id"
private const val KEY_CALENDAR_NAME = "planner_calendar_name"

/** Same SharedPreferences file as the theme settings; times are stored as minutes after midnight. */
@Singleton
class SharedPreferencesPlannerSettingsRepository
@Inject
constructor(@ApplicationContext context: Context) :
    PlannerSettingsRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _weeksAhead = MutableStateFlow(prefs.getInt(KEY_WEEKS_AHEAD, DEFAULT_WEEKS_AHEAD))
    override val weeksAhead: StateFlow<Int> = _weeksAhead

    override fun setWeeksAhead(weeks: Int) {
        val clamped = weeks.coerceIn(WEEKS_AHEAD_RANGE)
        prefs.edit { putInt(KEY_WEEKS_AHEAD, clamped) }
        _weeksAhead.value = clamped
    }

    private val _mealsPerDay = MutableStateFlow(prefs.getInt(KEY_MEALS_PER_DAY, DEFAULT_MEALS_PER_DAY))
    override val mealsPerDay: StateFlow<Int> = _mealsPerDay

    override fun setMealsPerDay(count: Int) {
        val clamped = count.coerceIn(MealType.MEALS_PER_DAY_RANGE)
        prefs.edit { putInt(KEY_MEALS_PER_DAY, clamped) }
        _mealsPerDay.value = clamped
    }

    private val _mealTimes =
        MutableStateFlow(
            MealType.entries.associateWith {
                readTime(KEY_MEAL_TIME_PREFIX + it.name, DEFAULT_MEAL_TIMES.getValue(it))
            }
        )
    override val mealTimes: StateFlow<Map<MealType, LocalTime>> = _mealTimes

    override fun setMealTime(mealType: MealType, time: LocalTime) {
        writeTime(KEY_MEAL_TIME_PREFIX + mealType.name, time)
        _mealTimes.value = _mealTimes.value + (mealType to time)
    }

    private val _groceryTime = MutableStateFlow(readTime(KEY_GROCERY_TIME, DEFAULT_GROCERY_TIME))
    override val groceryTime: StateFlow<LocalTime> = _groceryTime

    override fun setGroceryTime(time: LocalTime) {
        writeTime(KEY_GROCERY_TIME, time)
        _groceryTime.value = time
    }

    private val _calendar = MutableStateFlow(readCalendar())
    override val calendar: StateFlow<SelectedCalendar?> = _calendar

    override fun setCalendar(calendar: SelectedCalendar?) {
        prefs.edit {
            if (calendar == null) {
                remove(KEY_CALENDAR_ID)
                remove(KEY_CALENDAR_NAME)
            } else {
                putLong(KEY_CALENDAR_ID, calendar.id)
                putString(KEY_CALENDAR_NAME, calendar.displayName)
            }
        }
        _calendar.value = calendar
    }

    private fun readCalendar(): SelectedCalendar? {
        if (!prefs.contains(KEY_CALENDAR_ID)) return null
        return SelectedCalendar(prefs.getLong(KEY_CALENDAR_ID, -1), prefs.getString(KEY_CALENDAR_NAME, null).orEmpty())
    }

    private fun readTime(key: String, default: LocalTime): LocalTime {
        val minutes = prefs.getInt(key, -1)
        return if (minutes in 0 until 24 * 60) LocalTime.of(minutes / 60, minutes % 60) else default
    }

    private fun writeTime(key: String, time: LocalTime) = prefs.edit { putInt(key, time.hour * 60 + time.minute) }
}
