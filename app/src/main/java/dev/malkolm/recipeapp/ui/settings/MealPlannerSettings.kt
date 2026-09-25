package dev.malkolm.recipeapp.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.data.calendar.DeviceCalendar
import dev.malkolm.recipeapp.data.calendar.labelRes
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val calendarPermissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

/** What the time picker is open for: a meal, or the grocery day (`null`). */
private data class EditingTime(val mealType: MealType?, val current: LocalTime)

/** The "Meal planner" section of Settings. */
@Composable
fun MealPlannerSettings(viewModel: SettingsViewModel, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val weeksAhead by viewModel.weeksAhead.collectAsStateWithLifecycle()
    val mealsPerDay by viewModel.mealsPerDay.collectAsStateWithLifecycle()
    val mealTimes by viewModel.mealTimes.collectAsStateWithLifecycle()
    val groceryTime by viewModel.groceryTime.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    var editingTime by remember { mutableStateOf<EditingTime?>(null) }
    var calendarChoices by remember { mutableStateOf<List<DeviceCalendar>?>(null) }
    val calendarFailed = stringResource(R.string.settings_calendar_failed)
    val permissionDenied = stringResource(R.string.settings_calendar_permission_denied)

    fun showCalendarChoices() {
        scope.launch {
            viewModel.writableCalendars()
                .onSuccess { calendarChoices = it }
                .onFailure { onMessage(calendarFailed) }
        }
    }

    val requestPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) showCalendarChoices() else onMessage(permissionDenied)
        }

    Text(stringResource(R.string.settings_planner_title), style = MaterialTheme.typography.titleSmall)

    Text(pluralStringResource(R.plurals.settings_planner_weeks, weeksAhead, weeksAhead))
    val weeksRange = PlannerSettingsRepository.WEEKS_AHEAD_RANGE
    Slider(
        value = weeksAhead.toFloat(),
        onValueChange = { viewModel.setWeeksAhead(it.roundToInt()) },
        valueRange = weeksRange.first.toFloat()..weeksRange.last.toFloat(),
        steps = weeksRange.last - weeksRange.first - 1
    )

    Text(stringResource(R.string.settings_planner_meals_per_day))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (count in MealType.MEALS_PER_DAY_RANGE) {
            FilterChip(
                selected = count == mealsPerDay,
                onClick = { viewModel.setMealsPerDay(count) },
                label = { Text(count.toString()) }
            )
        }
    }
    Text(
        MealType.activeFor(mealsPerDay).map { stringResource(it.labelRes()) }.joinToString(", "),
        style = MaterialTheme.typography.bodySmall
    )

    Text(stringResource(R.string.settings_planner_times), style = MaterialTheme.typography.bodyMedium)
    for (mealType in MealType.activeFor(mealsPerDay)) {
        val time = mealTimes[mealType] ?: continue
        TimeRow(stringResource(mealType.labelRes()), time) { editingTime = EditingTime(mealType, time) }
    }
    TimeRow(stringResource(R.string.settings_planner_grocery_time), groceryTime) {
        editingTime = EditingTime(null, groceryTime)
    }

    Text(
        calendar?.let { stringResource(R.string.settings_calendar_on, it.displayName) }
            ?: stringResource(R.string.settings_calendar_off)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                val granted =
                    calendarPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                if (granted) showCalendarChoices() else requestPermissions.launch(calendarPermissions)
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                stringResource(
                    if (calendar ==
                        null
                    ) {
                        R.string.settings_calendar_choose
                    } else {
                        R.string.settings_calendar_change
                    }
                )
            )
        }
        if (calendar != null) {
            OutlinedButton(
                onClick = {
                    scope.launch { viewModel.chooseCalendar(null).onFailure { onMessage(calendarFailed) } }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.settings_calendar_stop))
            }
        }
    }

    editingTime?.let { editing ->
        TimeDialog(
            initial = editing.current,
            onDismiss = { editingTime = null },
            onConfirm = { time ->
                if (editing.mealType ==
                    null
                ) {
                    viewModel.setGroceryTime(time)
                } else {
                    viewModel.setMealTime(editing.mealType, time)
                }
                editingTime = null
            }
        )
    }
    calendarChoices?.let { choices ->
        CalendarChoiceDialog(
            calendars = choices,
            onDismiss = { calendarChoices = null },
            onPick = { picked ->
                calendarChoices = null
                scope.launch { viewModel.chooseCalendar(picked).onFailure { onMessage(calendarFailed) } }
            }
        )
    }
}

@Composable
private fun TimeRow(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current)
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.recipe_edit_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}

@Composable
private fun CalendarChoiceDialog(
    calendars: List<DeviceCalendar>,
    onDismiss: () -> Unit,
    onPick: (DeviceCalendar) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_calendar_choose)) },
        text = {
            if (calendars.isEmpty()) {
                Text(stringResource(R.string.settings_calendar_none))
            } else {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    calendars.forEach { calendar ->
                        TextButton(onClick = { onPick(calendar) }, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(calendar.displayName)
                                Text(calendar.accountName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}
