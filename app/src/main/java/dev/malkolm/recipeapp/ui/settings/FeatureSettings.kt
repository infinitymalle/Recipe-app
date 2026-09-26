package dev.malkolm.recipeapp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.Feature

/** The "Features" section of Settings: a switch for each optional feature. */
@Composable
fun FeatureSettings(viewModel: SettingsViewModel) {
    val switchedOn by viewModel.featuresSwitchedOn.collectAsStateWithLifecycle()
    Text(stringResource(R.string.settings_features_intro), style = MaterialTheme.typography.bodyMedium)
    Feature.entries.filter { it.isSwitchable }.forEach { feature ->
        FeatureRow(
            feature = feature,
            on = feature in switchedOn,
            onChange = { viewModel.setFeatureSwitchedOn(feature, it) }
        )
    }
    Text(stringResource(R.string.settings_feature_grocery_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun FeatureRow(feature: Feature, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(feature.titleRes()), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(feature.descriptionRes()), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}

private fun Feature.titleRes(): Int = when (this) {
    Feature.MEAL_PLANNER -> R.string.settings_feature_planner
    Feature.SHOPPING_LIST -> R.string.settings_feature_shopping
    Feature.COOK_MODE -> R.string.settings_feature_cook
    Feature.GROCERY_TO_SHOPPING_LIST -> R.string.settings_feature_grocery_note
}

private fun Feature.descriptionRes(): Int = when (this) {
    Feature.MEAL_PLANNER -> R.string.settings_feature_planner_description
    Feature.SHOPPING_LIST -> R.string.settings_feature_shopping_description
    Feature.COOK_MODE -> R.string.settings_feature_cook_description
    Feature.GROCERY_TO_SHOPPING_LIST -> R.string.settings_feature_grocery_note
}
