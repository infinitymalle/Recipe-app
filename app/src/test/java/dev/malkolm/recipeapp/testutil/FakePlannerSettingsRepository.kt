package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.SelectedCalendar
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [PlannerSettingsRepository] with the real defaults. */
class FakePlannerSettingsRepository : PlannerSettingsRepository {
    override val weeksAhead = MutableStateFlow(PlannerSettingsRepository.DEFAULT_WEEKS_AHEAD)
    override val mealsPerDay = MutableStateFlow(PlannerSettingsRepository.DEFAULT_MEALS_PER_DAY)
    override val mealTimes = MutableStateFlow(PlannerSettingsRepository.DEFAULT_MEAL_TIMES)
    override val groceryTime = MutableStateFlow(PlannerSettingsRepository.DEFAULT_GROCERY_TIME)
    override val calendar = MutableStateFlow<SelectedCalendar?>(null)

    override fun setWeeksAhead(weeks: Int) {
        weeksAhead.value = weeks.coerceIn(PlannerSettingsRepository.WEEKS_AHEAD_RANGE)
    }

    override fun setMealsPerDay(count: Int) {
        mealsPerDay.value = count.coerceIn(MealType.MEALS_PER_DAY_RANGE)
    }

    override fun setMealTime(mealType: MealType, time: LocalTime) {
        mealTimes.value = mealTimes.value + (mealType to time)
    }

    override fun setGroceryTime(time: LocalTime) {
        groceryTime.value = time
    }

    override fun setCalendar(calendar: SelectedCalendar?) {
        this.calendar.value = calendar
    }
}
