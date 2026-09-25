package dev.malkolm.recipeapp.data.backup

import dev.malkolm.recipeapp.data.AppFiles
import dev.malkolm.recipeapp.data.RecipeImageStorage
import dev.malkolm.recipeapp.data.calendar.MealPlanCalendarSync
import dev.malkolm.recipeapp.data.local.dao.PlanDao
import dev.malkolm.recipeapp.data.local.entity.PlanEntryEntity
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** The parts of a full backup besides recipes. */
data class BackupExtrasData(val mealPlan: List<BackupPlanEntry>, val settings: BackupSettings?)

/**
 * Saves and restores everything in a full backup besides the recipes: the meal plan and the
 * settings. Kept apart from [RecipeBackupService] so that class stays about recipes and zip files.
 */
interface BackupExtras {
    /** [addFile] copies a file (relative to the files directory) into the backup; `null` if it is missing. */
    suspend fun export(addFile: (String) -> String?): BackupExtrasData

    suspend fun restore(data: BackupExtrasData)

    /** For tests and single shared recipes: nothing besides the recipes. */
    object None : BackupExtras {
        override suspend fun export(addFile: (String) -> String?) = BackupExtrasData(emptyList(), null)

        override suspend fun restore(data: BackupExtrasData) = Unit
    }
}

class AppBackupExtras
@Inject
constructor(
    private val planDao: PlanDao,
    private val themeSettings: ThemeSettingsRepository,
    private val plannerSettings: PlannerSettingsRepository,
    private val imageStorage: RecipeImageStorage,
    private val calendarSync: MealPlanCalendarSync,
    private val clock: Clock
) : BackupExtras {
    override suspend fun export(addFile: (String) -> String?): BackupExtrasData {
        val mealPlan =
            planDao.getAllLive().map { entry ->
                BackupPlanEntry(
                    id = entry.id,
                    date = LocalDate.ofEpochDay(entry.epochDay).toString(),
                    kind = entry.kind,
                    mealType = entry.mealType,
                    recipeId = entry.recipeId
                )
            }
        val settings =
            BackupSettings(
                themeMode = themeSettings.themeMode.value.name,
                backgroundBlur = themeSettings.backgroundBlur.value,
                shoppingListImagePath = themeSettings.shoppingListImagePath.value?.let(addFile),
                weeksAhead = plannerSettings.weeksAhead.value,
                mealsPerDay = plannerSettings.mealsPerDay.value,
                mealTimes = plannerSettings.mealTimes.value.map { (type, time) ->
                    type.name to time.toString()
                }.toMap(),
                groceryTime = plannerSettings.groceryTime.value.toString()
            )
        return BackupExtrasData(mealPlan, settings)
    }

    /**
     * Restores what the backup has; anything missing or not understood is skipped, never trusted.
     * The meal plan is merged in: an entry with the same id is updated, and a meal replaces
     * whatever this phone had planned in the same slot. The calendar is then brought up to date.
     */
    override suspend fun restore(data: BackupExtrasData) {
        data.mealPlan.forEach { restorePlanEntry(it) }
        data.settings?.let { restoreSettings(it) }
        calendarSync.sync()
    }

    private suspend fun restorePlanEntry(entry: BackupPlanEntry) {
        if (!AppFiles.isSafeId(entry.id)) return
        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return
        val mealType = entry.mealType?.let { name -> MealType.entries.firstOrNull { it.name == name } }
        when (entry.kind) {
            PlanEntryEntity.KIND_MEAL -> if (mealType == null || entry.recipeId == null) return
            PlanEntryEntity.KIND_GROCERY -> if (entry.mealType != null) return
            else -> return
        }
        val now = clock.millis()
        planDao.findLive(date.toEpochDay(), entry.kind, mealType?.name)
            ?.takeIf { it.id != entry.id }
            ?.let { planDao.markDeleted(it.id, now) }
        planDao.upsert(
            PlanEntryEntity(
                id = entry.id,
                epochDay = date.toEpochDay(),
                kind = entry.kind,
                mealType = mealType?.name,
                recipeId = entry.recipeId,
                // Keeps this phone's calendar event for the entry, if it already had one.
                calendarEventId = planDao.getById(entry.id)?.calendarEventId,
                updatedAt = now,
                deletedAt = null
            )
        )
    }

    private fun restoreSettings(settings: BackupSettings) {
        settings.themeMode
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?.let(themeSettings::setThemeMode)
        settings.backgroundBlur?.let(themeSettings::setBackgroundBlur)
        settings.shoppingListImagePath
            ?.takeIf {
                it.startsWith("backgrounds/") && AppFiles.isSafeRelativePath(it) &&
                    imageStorage.resolve(it).isFile
            }
            ?.let(themeSettings::setShoppingListImagePath)
        settings.weeksAhead?.let(plannerSettings::setWeeksAhead)
        settings.mealsPerDay?.let(plannerSettings::setMealsPerDay)
        settings.mealTimes.forEach { (name, text) ->
            val type = MealType.entries.firstOrNull { it.name == name } ?: return@forEach
            parseTime(text)?.let { plannerSettings.setMealTime(type, it) }
        }
        settings.groceryTime?.let(::parseTime)?.let(plannerSettings::setGroceryTime)
    }

    private fun parseTime(text: String): LocalTime? = runCatching { LocalTime.parse(text) }.getOrNull()
}
