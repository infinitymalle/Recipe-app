package dev.malkolm.recipeapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.data.RecipeImageStorage
import dev.malkolm.recipeapp.data.backup.RecipeBackupService
import dev.malkolm.recipeapp.data.calendar.CalendarGateway
import dev.malkolm.recipeapp.data.calendar.DeviceCalendar
import dev.malkolm.recipeapp.data.calendar.MealPlanCalendarSync
import dev.malkolm.recipeapp.data.examples.ExampleRecipes
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.Feature
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.FeatureSettingsRepository
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.domain.repository.SelectedCalendar
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val backupService: RecipeBackupService,
    private val themeSettingsRepository: ThemeSettingsRepository,
    private val imageStorage: RecipeImageStorage,
    private val recipeRepository: RecipeRepository,
    private val idGenerator: IdGenerator,
    private val plannerSettings: PlannerSettingsRepository,
    private val calendarSync: MealPlanCalendarSync,
    private val calendarGateway: CalendarGateway,
    private val featureSettings: FeatureSettingsRepository
) : ViewModel() {
    val featuresSwitchedOn: StateFlow<Set<Feature>> = featureSettings.switchedOn

    fun setFeatureSwitchedOn(feature: Feature, on: Boolean) {
        featureSettings.setSwitchedOn(feature, on)
        // The planner's calendar events leave (or come back) with the planner.
        if (feature == Feature.MEAL_PLANNER) syncCalendar()
    }

    val themeMode: StateFlow<ThemeMode> = themeSettingsRepository.themeMode
    val backgroundBlur: StateFlow<Int> = themeSettingsRepository.backgroundBlur
    val shoppingListImagePath: StateFlow<String?> = themeSettingsRepository.shoppingListImagePath

    val weeksAhead: StateFlow<Int> = plannerSettings.weeksAhead
    val mealsPerDay: StateFlow<Int> = plannerSettings.mealsPerDay
    val mealTimes: StateFlow<Map<MealType, LocalTime>> = plannerSettings.mealTimes
    val groceryTime: StateFlow<LocalTime> = plannerSettings.groceryTime
    val calendar: StateFlow<SelectedCalendar?> = plannerSettings.calendar

    fun setWeeksAhead(weeks: Int) = plannerSettings.setWeeksAhead(weeks)

    // The settings below change which events exist or when they start, so the calendar follows.
    fun setMealsPerDay(count: Int) {
        plannerSettings.setMealsPerDay(count)
        syncCalendar()
    }

    fun setMealTime(mealType: MealType, time: LocalTime) {
        plannerSettings.setMealTime(mealType, time)
        syncCalendar()
    }

    fun setGroceryTime(time: LocalTime) {
        plannerSettings.setGroceryTime(time)
        syncCalendar()
    }

    /** The phone's calendars to choose from; needs the calendar permissions already granted. */
    suspend fun writableCalendars(): Result<List<DeviceCalendar>> =
        withContext(Dispatchers.IO) { runCatching { calendarGateway.writableCalendars() } }

    /** Copies the plan into [calendar] from now on; `null` stops and removes the upcoming events. */
    suspend fun chooseCalendar(calendar: DeviceCalendar?): Result<Unit> =
        calendarSync.switchCalendar(calendar?.let { SelectedCalendar(it.id, it.displayName) })

    private fun syncCalendar() {
        viewModelScope.launch { calendarSync.sync() }
    }

    fun setThemeMode(mode: ThemeMode) = themeSettingsRepository.setThemeMode(mode)

    fun setBackgroundBlur(dp: Int) = themeSettingsRepository.setBackgroundBlur(dp)

    /** Copies the picked photo into app storage and makes it the shopping list background. */
    suspend fun setShoppingListImage(source: Uri): Result<Unit> = runCatching {
        val newPath = imageStorage.saveBackgroundImage(source)
        val oldPath = shoppingListImagePath.value
        themeSettingsRepository.setShoppingListImagePath(newPath)
        oldPath?.let { imageStorage.delete(it) }
    }

    suspend fun removeShoppingListImage() {
        val oldPath = shoppingListImagePath.value ?: return
        themeSettingsRepository.setShoppingListImagePath(null)
        imageStorage.delete(oldPath)
    }

    /**
     * Adds the built-in example recipes, tagged "Example". They have fixed ids, so adding them
     * again updates (or restores) the same recipes rather than creating duplicates.
     */
    suspend fun addExampleRecipes(): Result<Int> = runCatching {
        val drafts = ExampleRecipes.all(idGenerator::newId)
        drafts.forEach { recipeRepository.saveRecipe(it) }
        drafts.size
    }

    suspend fun exportBackup(destination: Uri): Result<Int> = runCatching { backupService.export(destination) }

    suspend fun importBackup(source: Uri): Result<Int> = runCatching { backupService.import(source) }
}
