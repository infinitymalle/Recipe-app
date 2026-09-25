package dev.malkolm.recipeapp.ui.cookrecipe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.durationsIn
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme

/** Stateful entry point: connects the ViewModel to the stateless [CookRecipeContent]. */
@Composable
fun CookRecipeScreen(onBack: () -> Unit, viewModel: CookRecipeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Timers keep running via a foreground-service notification (see CookTimerService); without
    // this permission (Android 13+) the timers still run, they just cannot show that notification.
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CookRecipeContent(
        uiState = uiState,
        onBack = onBack,
        onPreviousStep = viewModel::previousStep,
        onNextStep = viewModel::nextStep,
        onAddTimer = viewModel::addTimer,
        onAdjustTimer = viewModel::adjustTimer,
        onToggleTimer = viewModel::toggleTimer,
        onRemoveTimer = viewModel::removeTimer
    )
}

/** Stateless: renders whatever [uiState] says. Easy to preview and to test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookRecipeContent(
    uiState: CookRecipeUiState,
    onBack: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onAddTimer: (minutes: Int, label: String) -> Unit,
    onAdjustTimer: (id: String, deltaMinutes: Int) -> Unit,
    onToggleTimer: (String) -> Unit,
    onRemoveTimer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddTimer by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (uiState as? CookRecipeUiState.Content)?.title ?: stringResource(R.string.app_name)
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.recipe_detail_back)) }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            CookRecipeUiState.Loading -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding))

            CookRecipeUiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.recipe_detail_not_found))
                }
            }

            is CookRecipeUiState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { StepCard(uiState, onPreviousStep, onNextStep, onAddTimer) }
                    item {
                        Text(
                            stringResource(R.string.cook_recipe_timers_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    items(uiState.timers, key = { it.id }) { timer ->
                        TimerRow(
                            timer = timer,
                            onAdjust = { delta -> onAdjustTimer(timer.id, delta) },
                            onToggle = { onToggleTimer(timer.id) },
                            onRemove = { onRemoveTimer(timer.id) }
                        )
                    }
                    item {
                        OutlinedButton(onClick = { showAddTimer = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.cook_recipe_add_timer))
                        }
                    }
                }

                if (showAddTimer) {
                    AddTimerDialog(
                        initialMinutes = uiState.suggestedTimerMinutes,
                        onDismiss = { showAddTimer = false },
                        onAdd = { minutes, label ->
                            onAddTimer(minutes, label)
                            showAddTimer = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    state: CookRecipeUiState.Content,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onAddTimer: (minutes: Int, label: String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.steps.isEmpty()) {
                Text(stringResource(R.string.cook_recipe_no_steps))
            } else {
                Text(
                    stringResource(
                        R.string.cook_recipe_step_label,
                        state.currentStepIndex + 1,
                        state.steps.size
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
                val step = state.steps[state.currentStepIndex]
                Text(step, style = MaterialTheme.typography.bodyLarge)
                // One tap starts a timer for each time the step mentions ("Bake for 8 to 10 minutes").
                val durations = durationsIn(step)
                if (durations.isNotEmpty()) {
                    val stepLabel = stringResource(R.string.cook_recipe_step_timer_label, state.currentStepIndex + 1)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        durations.forEach { duration ->
                            AssistChip(
                                onClick = { onAddTimer(duration.minutes, stepLabel) },
                                label = {
                                    Text(stringResource(R.string.cook_recipe_start_step_timer, duration.minutes))
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPreviousStep, enabled = state.currentStepIndex > 0) {
                        Text(stringResource(R.string.cook_recipe_previous))
                    }
                    OutlinedButton(onClick = onNextStep, enabled = state.currentStepIndex < state.steps.lastIndex) {
                        Text(stringResource(R.string.cook_recipe_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerRow(
    timer: CookTimer,
    onAdjust: (deltaMinutes: Int) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            val label = timer.label.ifBlank { stringResource(R.string.cook_recipe_timer_default_label) }
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(formatTimer(timer.remainingSeconds), style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onAdjust(-1) }) { Text(stringResource(R.string.cook_recipe_timer_minus)) }
                TextButton(onClick = { onAdjust(1) }) { Text(stringResource(R.string.cook_recipe_timer_plus)) }
                TextButton(onClick = onToggle, enabled = timer.remainingSeconds > 0) {
                    Text(
                        stringResource(
                            if (timer.isRunning) R.string.cook_recipe_pause else R.string.cook_recipe_resume
                        )
                    )
                }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.recipe_edit_remove_attachment)) }
            }
        }
    }
}

private fun formatTimer(remainingSeconds: Int): String {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun AddTimerDialog(initialMinutes: Int?, onDismiss: () -> Unit, onAdd: (minutes: Int, label: String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf(initialMinutes?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cook_recipe_add_timer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.cook_recipe_timer_label)) }
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it },
                    label = { Text(stringResource(R.string.cook_recipe_timer_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            val minutes = minutesText.toIntOrNull()
            TextButton(onClick = { onAdd(minutes ?: 0, label) }, enabled = minutes != null && minutes > 0) {
                Text(stringResource(R.string.recipe_edit_dialog_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recipe_edit_dialog_cancel)) } }
    )
}

@Preview(showBackground = true)
@Composable
private fun CookRecipeContentPreview() {
    RecipeAppTheme {
        CookRecipeContent(
            uiState =
                CookRecipeUiState.Content(
                    title = "Sourdough bread",
                    steps = listOf("Mix flour, water and starter.", "Knead for 10 minutes.", "Let it proof overnight."),
                    currentStepIndex = 1,
                    suggestedTimerMinutes = 45,
                    timers =
                        listOf(
                            CookTimer(
                                id = "t1",
                                label = "Proofing",
                                totalSeconds = 2700,
                                remainingSeconds = 1500,
                                isRunning = true
                            )
                        )
                ),
            onBack = {},
            onPreviousStep = {},
            onNextStep = {},
            onAddTimer = { _, _ -> },
            onAdjustTimer = { _, _ -> },
            onToggleTimer = {},
            onRemoveTimer = {}
        )
    }
}
