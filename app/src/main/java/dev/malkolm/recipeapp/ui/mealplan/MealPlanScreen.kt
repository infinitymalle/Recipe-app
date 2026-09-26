package dev.malkolm.recipeapp.ui.mealplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CalendarStatus(uiState) }
            items(uiState.days, key = { it.date.toEpochDay() }) { day ->
                DayCard(
                    day = day,
                    dayLabel = dayLabel(day.date, uiState.today),
                    onOpenRecipe = onOpenRecipe,
                    onPick = { mealType -> pickingFor = PickingFor(day.date, mealType) },
                    onRemove = { mealType -> onRemoveMeal(day.date, mealType) },
                    onSetGroceryDay = { onSetGroceryDay(day.date, it) },
                    onAddGroceries = { onAddGroceries(day.date) }
                )
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

@Composable
private fun CalendarStatus(uiState: MealPlanUiState) {
    val text =
        when {
            uiState.calendarSyncFailed -> stringResource(R.string.meal_plan_calendar_failed)
            uiState.calendarName != null -> stringResource(R.string.meal_plan_calendar_on, uiState.calendarName)
            else -> stringResource(R.string.meal_plan_calendar_off)
        }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (uiState.calendarSyncFailed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
    )
}

@Composable
private fun DayCard(
    day: DayPlan,
    dayLabel: String,
    onOpenRecipe: (String) -> Unit,
    onPick: (MealType) -> Unit,
    onRemove: (MealType) -> Unit,
    onSetGroceryDay: (Boolean) -> Unit,
    onAddGroceries: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(dayLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = day.isGroceryDay,
                    onClick = { onSetGroceryDay(!day.isGroceryDay) },
                    label = { Text(stringResource(R.string.meal_plan_grocery_day)) }
                )
            }
            // Needs the shopping list too; hidden when that feature is switched off.
            if (day.isGroceryDay && Feature.GROCERY_TO_SHOPPING_LIST in LocalEnabledFeatures.current) {
                TextButton(onClick = onAddGroceries) { Text(stringResource(R.string.meal_plan_add_groceries)) }
            }
            day.meals.forEach { slot ->
                MealRow(
                    slot = slot,
                    onOpenRecipe = onOpenRecipe,
                    onPick = { onPick(slot.mealType) },
                    onRemove = { onRemove(slot.mealType) }
                )
            }
        }
    }
}

@Composable
private fun MealRow(slot: MealSlot, onOpenRecipe: (String) -> Unit, onPick: () -> Unit, onRemove: () -> Unit) {
    val mealLabel = stringResource(slot.mealType.labelRes())
    val meal = slot.meal
    if (meal == null) {
        TextButton(onClick = onPick) { Text(stringResource(R.string.meal_plan_add_meal, mealLabel)) }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f).clickable(enabled = meal.recipeTitle != null) {
                onOpenRecipe(meal.recipeId)
            }
        ) {
            Text(mealLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                meal.recipeTitle ?: stringResource(R.string.meal_plan_deleted_recipe),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onPick) { Text(stringResource(R.string.meal_plan_change)) }
        TextButton(onClick = onRemove) { Text(stringResource(R.string.recipe_edit_remove_attachment)) }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM")

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
