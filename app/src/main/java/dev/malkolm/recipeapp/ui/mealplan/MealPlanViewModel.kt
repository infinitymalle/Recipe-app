package dev.malkolm.recipeapp.ui.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.data.calendar.MealPlanCalendarSync
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.PlanEntry
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.repository.MealPlanRepository
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.domain.repository.ShoppingListRepository
import dev.malkolm.recipeapp.ui.shoppinglist.shoppingEntriesFrom
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One meal of a day: the planned recipe, or `null` for an empty slot. */
data class MealSlot(val mealType: MealType, val meal: PlanEntry.Meal?)

data class DayPlan(val date: LocalDate, val meals: List<MealSlot>, val isGroceryDay: Boolean)

/** Result of "add ingredients" on a grocery day, for a confirmation message. */
data class GroceriesAdded(val meals: Int, val ingredients: Int)

data class MealPlanUiState(
    val today: LocalDate,
    val days: List<DayPlan> = emptyList(),
    /** For the recipe picker. */
    val recipes: List<RecipeSummary> = emptyList(),
    /** The calendar the plan is copied into, or `null` when calendar sync is off. */
    val calendarName: String? = null,
    /** The last calendar update failed, e.g. the calendar permission was taken away. */
    val calendarSyncFailed: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealPlanViewModel
@Inject
constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val settings: PlannerSettingsRepository,
    private val recipeRepository: RecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val calendarSync: MealPlanCalendarSync,
    clock: Clock
) : ViewModel() {
    private val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
    private val calendarSyncFailed = MutableStateFlow(false)

    private val _groceriesAdded = MutableSharedFlow<GroceriesAdded>(extraBufferCapacity = 1)
    val groceriesAdded: Flow<GroceriesAdded> = _groceriesAdded.asSharedFlow()

    private val days =
        combine(settings.weeksAhead, settings.mealsPerDay) { weeks, meals -> weeks to meals }
            .flatMapLatest { (weeks, mealsPerDay) ->
                val lastDay = today.plusDays(weeks * 7L - 1)
                val activeMeals = MealType.activeFor(mealsPerDay)
                mealPlanRepository.observePlan(today, lastDay).map { entries ->
                    daysFrom(today, lastDay, entries, activeMeals)
                }
            }

    val uiState: StateFlow<MealPlanUiState> =
        combine(
            days,
            recipeRepository.observeRecipeSummaries(),
            settings.calendar,
            calendarSyncFailed
        ) { days, recipes, calendar, syncFailed ->
            MealPlanUiState(
                today = today,
                days = days,
                recipes = recipes,
                calendarName = calendar?.displayName,
                calendarSyncFailed = syncFailed
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealPlanUiState(today = today))

    init {
        // Catches changes made elsewhere since the last sync, e.g. a renamed or deleted recipe.
        syncCalendar()
    }

    fun setMeal(date: LocalDate, mealType: MealType, recipeId: String) = changeThenSync {
        mealPlanRepository.setMeal(date, mealType, recipeId)
    }

    fun removeMeal(date: LocalDate, mealType: MealType) = changeThenSync {
        mealPlanRepository.removeMeal(date, mealType)
    }

    fun setGroceryDay(date: LocalDate, isGroceryDay: Boolean) = changeThenSync {
        mealPlanRepository.setGroceryDay(date, isGroceryDay)
    }

    /**
     * Adds the ingredients of every meal planned from the grocery day [date] up to (not including)
     * the next grocery day, or to the end of the plan if there is none, to the shopping list.
     * Amounts of the same ingredient are combined by the shopping list.
     */
    fun addGroceriesFor(date: LocalDate) {
        viewModelScope.launch {
            val lastDay = today.plusDays(settings.weeksAhead.value * 7L - 1)
            val entries = mealPlanRepository.observePlan(date, maxOf(date, lastDay)).first()
            val meals = mealsForGroceryTrip(date, entries, MealType.activeFor(settings.mealsPerDay.value))
            val ingredients =
                meals.flatMap { meal ->
                    recipeRepository.observeRecipe(meal.recipeId).first()?.let {
                        shoppingEntriesFrom(it.ingredients)
                    }.orEmpty()
                }
            if (ingredients.isNotEmpty()) shoppingListRepository.addEntries(ingredients)
            _groceriesAdded.tryEmit(GroceriesAdded(meals = meals.size, ingredients = ingredients.size))
        }
    }

    private fun changeThenSync(change: suspend () -> Unit) {
        viewModelScope.launch {
            change()
            calendarSyncFailed.value = calendarSync.sync().isFailure
        }
    }

    private fun syncCalendar() {
        viewModelScope.launch { calendarSyncFailed.value = calendarSync.sync().isFailure }
    }
}

/**
 * The meals a grocery trip on [groceryDay] shops for: from that day (dinner is usually cooked after
 * shopping) up to the day before the next grocery day in [entries]. Only meals currently shown
 * ([activeMeals]) whose recipe still exists.
 */
internal fun mealsForGroceryTrip(
    groceryDay: LocalDate,
    entries: List<PlanEntry>,
    activeMeals: List<MealType>
): List<PlanEntry.Meal> {
    val nextGroceryDay =
        entries.filterIsInstance<PlanEntry.GroceryTrip>().map { it.date }.filter { it.isAfter(groceryDay) }.minOrNull()
    return entries
        .filterIsInstance<PlanEntry.Meal>()
        .filter { !it.date.isBefore(groceryDay) && (nextGroceryDay == null || it.date.isBefore(nextGroceryDay)) }
        .filter { it.mealType in activeMeals && it.recipeTitle != null }
        .sortedWith(compareBy({ it.date }, { it.mealType }))
}

/** Every day from [first] to [last], with each active meal's plan and whether it is a grocery day. */
internal fun daysFrom(
    first: LocalDate,
    last: LocalDate,
    entries: List<PlanEntry>,
    activeMeals: List<MealType>
): List<DayPlan> {
    val byDate = entries.groupBy { it.date }
    return generateSequence(first) { it.plusDays(1) }
        .takeWhile { !it.isAfter(last) }
        .map { date ->
            val dayEntries = byDate[date].orEmpty()
            val meals = dayEntries.filterIsInstance<PlanEntry.Meal>()
            DayPlan(
                date = date,
                meals = activeMeals.map { type -> MealSlot(type, meals.firstOrNull { it.mealType == type }) },
                isGroceryDay = dayEntries.any { it is PlanEntry.GroceryTrip }
            )
        }
        .toList()
}
