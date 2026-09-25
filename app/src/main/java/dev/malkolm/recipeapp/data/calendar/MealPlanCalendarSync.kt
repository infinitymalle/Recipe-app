package dev.malkolm.recipeapp.data.calendar

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.data.local.dao.PlanDao
import dev.malkolm.recipeapp.data.local.dao.PlanEntryRow
import dev.malkolm.recipeapp.data.local.entity.PlanEntryEntity
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.SelectedCalendar
import dev.malkolm.recipeapp.ui.recipeedit.IngredientListItem
import dev.malkolm.recipeapp.ui.recipeedit.ingredientItemsFrom
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Copies the meal plan into the chosen phone calendar, one way (app to calendar): every planned
 * meal and grocery day from today on becomes a 1-hour event at its time from the settings, with
 * no reminder. Changing or removing it in the app updates or deletes the event; editing the event
 * in the calendar app is not read back, and the next sync writes the plan's version again.
 *
 * [sync] compares the whole plan with the calendar each time instead of tracking single changes,
 * so it also catches settings changes (times, meals per day), renamed or deleted recipes, and
 * events the user deleted. Past days are left alone, so they stay in the calendar as a history.
 */
@Singleton
class MealPlanCalendarSync
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val dao: PlanDao,
    private val settings: PlannerSettingsRepository,
    private val gateway: CalendarGateway,
    private val clock: Clock
) {
    // Syncs are started from several screens; running two at once could create an event twice.
    private val mutex = Mutex()

    /** Brings the calendar in line with the plan. Fails (e.g. calendar permission taken away) without crashing. */
    suspend fun sync(): Result<Unit> = mutex.withLock { withContext(Dispatchers.IO) { runCatching { syncLocked() } } }

    /**
     * Moves the plan to [calendar] (or, for `null`, stops syncing): removes this app's future
     * events from the old calendar first, so none are left behind there.
     */
    suspend fun switchCalendar(calendar: SelectedCalendar?): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                settings.setCalendar(null)
                syncLocked()
                settings.setCalendar(calendar)
                syncLocked()
            }
        }
    }

    private suspend fun syncLocked() {
        val calendar = settings.calendar.value
        val zone = ZoneId.systemDefault()
        val today = clock.instant().atZone(zone).toLocalDate()
        val activeMeals = MealType.activeFor(settings.mealsPerDay.value)
        for (row in dao.getAllFrom(today.toEpochDay())) {
            val event = if (calendar != null && row.deletedAt == null) eventFor(row, activeMeals, zone) else null
            val eventId = row.calendarEventId
            when {
                event != null && calendar != null -> {
                    val newId = gateway.upsertEvent(calendar.id, eventId, event)
                    if (newId != eventId) dao.setCalendarEventId(row.id, newId)
                }

                eventId != null -> {
                    gateway.deleteEvent(eventId)
                    dao.setCalendarEventId(row.id, null)
                }
            }
        }
    }

    /** `null` when the entry should not be in the calendar (a hidden meal, or a deleted recipe). */
    private fun eventFor(row: PlanEntryRow, activeMeals: List<MealType>, zone: ZoneId): CalendarEvent? {
        val date = LocalDate.ofEpochDay(row.epochDay)
        return when (row.kind) {
            PlanEntryEntity.KIND_GROCERY ->
                event(context.getString(R.string.meal_plan_event_grocery), "", date, settings.groceryTime.value, zone)

            PlanEntryEntity.KIND_MEAL -> {
                val mealType = MealType.entries.firstOrNull { it.name == row.mealType } ?: return null
                val title = row.recipeTitle ?: return null
                if (mealType !in activeMeals) return null
                val time = settings.mealTimes.value[mealType] ?: return null
                event(
                    context.getString(R.string.meal_plan_event_meal, context.getString(mealType.labelRes()), title),
                    ingredientsDescription(row.recipeIngredients.orEmpty()),
                    date,
                    time,
                    zone
                )
            }

            else -> null
        }
    }

    private fun event(
        title: String,
        description: String,
        date: LocalDate,
        time: LocalTime,
        zone: ZoneId
    ): CalendarEvent {
        val start = date.atTime(time).atZone(zone)
        return CalendarEvent(title = title, description = description, start = start, end = start.plus(EVENT_LENGTH))
    }

    /** The recipe's ingredients as plain lines, so they can be read straight from the calendar. */
    private fun ingredientsDescription(ingredients: String): String =
        ingredientItemsFrom(ingredients) { "" }.joinToString("\n") { item ->
            when (item) {
                is IngredientListItem.Heading -> "${item.text}:"

                is IngredientListItem.Entry ->
                    if (item.amount.isNullOrBlank()) "• ${item.name}" else "• ${item.name} (${item.amount})"
            }
        }

    private companion object {
        val EVENT_LENGTH: Duration = Duration.ofHours(1)
    }
}

/** The display name of a meal, e.g. "Dinner". */
fun MealType.labelRes(): Int = when (this) {
    MealType.BREAKFAST -> R.string.meal_type_breakfast
    MealType.LUNCH -> R.string.meal_type_lunch
    MealType.DINNER -> R.string.meal_type_dinner
}
