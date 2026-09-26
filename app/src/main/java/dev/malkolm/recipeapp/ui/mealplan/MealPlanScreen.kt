package dev.malkolm.recipeapp.ui.mealplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.data.calendar.labelRes
import dev.malkolm.recipeapp.domain.model.Feature
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.PlanEntry
import dev.malkolm.recipeapp.ui.components.LocalEnabledFeatures
import dev.malkolm.recipeapp.ui.components.PickRecipeDialog
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/** Stateful entry point: connects the ViewModel to the stateless [MealPlanContent]. */
@Composable
fun MealPlanScreen(onOpenRecipe: (String) -> Unit, viewModel: MealPlanViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.groceriesAdded.collect { added ->
            val message =
                if (added.meals == 0) {
                    resources.getString(R.string.meal_plan_groceries_none)
                } else {
                    resources.getQuantityString(
                        R.plurals.meal_plan_groceries_added,
                        added.meals,
                        added.ingredients,
                        added.meals
                    )
                }
            snackbarHostState.showSnackbar(message)
        }
    }
    MealPlanContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddGroceries = viewModel::addGroceriesFor,
        onOpenRecipe = onOpenRecipe,
        onSetMeal = viewModel::setMeal,
        onRemoveMeal = viewModel::removeMeal,
        onSetGroceryDay = viewModel::setGroceryDay
    )
}

/** A slot being filled or changed: which day and meal the recipe picker is for. */
private data class PickingFor(val date: LocalDate, val mealType: MealType)

/** Stateless: renders whatever [uiState] says. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanContent(
    uiState: MealPlanUiState,
    onOpenRecipe: (String) -> Unit,
    onSetMeal: (LocalDate, MealType, String) -> Unit,
    onRemoveMeal: (LocalDate, MealType) -> Unit,
    onSetGroceryDay: (LocalDate, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAddGroceries: (LocalDate) -> Unit = {}
) {
    var pickingFor by remember { mutableStateOf<PickingFor?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meal_plan_title)) }
            )
        }
    ) { innerPadding ->
        // One page per Mon-Sun week; swipe sideways (or use the arrows) to change week.
        val weeks = uiState.days.chunked(7)
        val pagerState = rememberPagerState(pageCount = { weeks.size })
        val scope = rememberCoroutineScope()
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            CalendarStatus(uiState, modifier = Modifier.padding(horizontal = 16.dp))
            weeks.getOrNull(pagerState.currentPage)?.let { week ->
                WeekHeader(
                    week = week,
                    weekIndex = pagerState.currentPage,
                    canGoBack = pagerState.currentPage > 0,
                    canGoForward = pagerState.currentPage < weeks.lastIndex,
                    onBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    onForward = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                )
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(weeks[page], key = { it.date.toEpochDay() }) { day ->
                        DayCard(
                            day = day,
                            isToday = day.date == uiState.today,
                            isPast = day.date.isBefore(uiState.today),
                            onOpenRecipe = onOpenRecipe,
                            onPick = { mealType -> pickingFor = PickingFor(day.date, mealType) },
                            onRemove = { mealType -> onRemoveMeal(day.date, mealType) },
                            onSetGroceryDay = { onSetGroceryDay(day.date, it) },
                            onAddGroceries = { onAddGroceries(day.date) }
                        )
                    }
                }
            }
        }
    }

    pickingFor?.let { target ->
        PickRecipeDialog(
            title =
                stringResource(
                    R.string.meal_plan_pick_title,
                    stringResource(target.mealType.labelRes()),
                    dayLabel(target.date, uiState.today)
                ),
            recipes = uiState.recipes,
            onDismiss = { pickingFor = null },
            onPick = { recipeId ->
                onSetMeal(target.date, target.mealType, recipeId)
                pickingFor = null
            }
        )
    }
}

/** "This week", "Next week" or "Week of 12 Oct", with the week's dates and arrows to move. */
@Composable
private fun WeekHeader(
    week: List<DayPlan>,
    weekIndex: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit
) {
    val first = week.first().date
    val last = week.last().date
    val title =
        when (weekIndex) {
            0 -> stringResource(R.string.meal_plan_this_week)
            1 -> stringResource(R.string.meal_plan_next_week)
            else -> stringResource(R.string.meal_plan_week_of, first.format(shortDateFormat))
        }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, enabled = canGoBack) {
            Text(stringResource(R.string.meal_plan_previous_week), style = MaterialTheme.typography.headlineMedium)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${first.format(shortDateFormat)} – ${last.format(shortDateFormat)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = onForward, enabled = canGoForward) {
            Text(stringResource(R.string.meal_plan_next_week_arrow), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun CalendarStatus(uiState: MealPlanUiState, modifier: Modifier = Modifier) {
    val text =
        when {
            uiState.calendarSyncFailed -> stringResource(R.string.meal_plan_calendar_failed)
            uiState.calendarName != null -> stringResource(R.string.meal_plan_calendar_on, uiState.calendarName)
            else -> stringResource(R.string.meal_plan_calendar_off)
        }
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (uiState.calendarSyncFailed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
    )
}

/**
 * One day as a compact lane: the date on the left, its meals in the middle and the grocery-day
 * toggle on the right, so a whole week fits on the screen.
 */
@Composable
private fun DayCard(
    day: DayPlan,
    isToday: Boolean,
    isPast: Boolean,
    onOpenRecipe: (String) -> Unit,
    onPick: (MealType) -> Unit,
    onRemove: (MealType) -> Unit,
    onSetGroceryDay: (Boolean) -> Unit,
    onAddGroceries: () -> Unit
) {
    // Days already gone this week stay visible but faded.
    Card(modifier = Modifier.fillMaxWidth().alpha(if (isPast) 0.5f else 1f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.date.format(weekdayFormat), style = MaterialTheme.typography.labelMedium, color = dateColor)
                Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = dateColor)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                day.meals.forEach { slot ->
                    MealItem(
                        slot = slot,
                        showMealName = day.meals.size > 1,
                        onOpenRecipe = onOpenRecipe,
                        onPick = { onPick(slot.mealType) },
                        onRemove = { onRemove(slot.mealType) }
                    )
                }
                // Needs the shopping list too; hidden when that feature is switched off.
                if (day.isGroceryDay && Feature.GROCERY_TO_SHOPPING_LIST in LocalEnabledFeatures.current) {
                    TextButton(onClick = onAddGroceries, contentPadding = compactPadding) {
                        Text(
                            stringResource(R.string.meal_plan_add_groceries),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            FilterChip(
                selected = day.isGroceryDay,
                onClick = { onSetGroceryDay(!day.isGroceryDay) },
                label = { Text(stringResource(R.string.meal_plan_grocery_day)) }
            )
        }
    }
}

/**
 * A meal slot: "+ Dinner" when empty; otherwise the recipe's name, which opens a small menu to
 * open the recipe, change it or remove it (one tap target instead of three buttons per lane).
 */
@Composable
private fun MealItem(
    slot: MealSlot,
    showMealName: Boolean,
    onOpenRecipe: (String) -> Unit,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val mealLabel = stringResource(slot.mealType.labelRes())
    val meal = slot.meal
    if (meal == null) {
        TextButton(onClick = onPick, contentPadding = compactPadding) {
            Text(stringResource(R.string.meal_plan_add_meal, mealLabel))
        }
        return
    }
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Column(modifier = Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(vertical = 6.dp)) {
            if (showMealName) {
                Text(mealLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                meal.recipeTitle ?: stringResource(R.string.meal_plan_deleted_recipe),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (meal.recipeTitle != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.meal_plan_open_recipe)) },
                    onClick = {
                        menuOpen = false
                        onOpenRecipe(meal.recipeId)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.meal_plan_change)) },
                onClick = {
                    menuOpen = false
                    onPick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.recipe_edit_remove_attachment)) },
                onClick = {
                    menuOpen = false
                    onRemove()
                }
            )
        }
    }
}

private val compactPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
private val weekdayFormat = DateTimeFormatter.ofPattern("EEE")
private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM")
private val shortDateFormat = DateTimeFormatter.ofPattern("d MMM")

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.meal_plan_today, date.format(dateFormat))
    today.plusDays(1) -> stringResource(R.string.meal_plan_tomorrow, date.format(dateFormat))
    else -> date.format(dateFormat)
}

@Preview(showBackground = true)
@Composable
private fun MealPlanContentPreview() {
    val today = LocalDate.of(2026, 9, 25)
    RecipeAppTheme {
        MealPlanContent(
            uiState =
                MealPlanUiState(
                    today = today,
                    days =
                        daysFrom(
                            today,
                            today.plusDays(2),
                            listOf(
                                PlanEntry.Meal("1", today, MealType.DINNER, "r1", "Spaghetti bolognese"),
                                PlanEntry.GroceryTrip("2", today.plusDays(1))
                            ),
                            listOf(MealType.DINNER)
                        ),
                    calendarName = "Personal"
                ),
            onOpenRecipe = {},
            onSetMeal = { _, _, _ -> },
            onRemoveMeal = { _, _ -> },
            onSetGroceryDay = { _, _ -> }
        )
    }
}
