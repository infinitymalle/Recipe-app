package dev.malkolm.recipeapp.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.malkolm.recipeapp.domain.model.Feature
import dev.malkolm.recipeapp.domain.model.enabledFeatures
import dev.malkolm.recipeapp.domain.repository.FeatureSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "settings"
private const val KEY_PREFIX = "feature_"

/** Same SharedPreferences file as the other settings; one boolean per switchable feature. */
@Singleton
class SharedPreferencesFeatureSettingsRepository
@Inject
constructor(@ApplicationContext context: Context) :
    FeatureSettingsRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _switchedOn =
        MutableStateFlow(
            Feature.entries.filter {
                it.isSwitchable && prefs.getBoolean(KEY_PREFIX + it.name, true)
            }.toSet()
        )
    override val switchedOn: StateFlow<Set<Feature>> = _switchedOn

    private val _enabled = MutableStateFlow(enabledFeatures(_switchedOn.value))
    override val enabled: StateFlow<Set<Feature>> = _enabled

    override fun setSwitchedOn(feature: Feature, on: Boolean) {
        if (!feature.isSwitchable) return
        prefs.edit { putBoolean(KEY_PREFIX + feature.name, on) }
        _switchedOn.value = if (on) _switchedOn.value + feature else _switchedOn.value - feature
        _enabled.value = enabledFeatures(_switchedOn.value)
    }
}
