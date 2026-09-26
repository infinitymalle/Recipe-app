package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.domain.model.Feature
import dev.malkolm.recipeapp.domain.model.enabledFeatures
import dev.malkolm.recipeapp.domain.repository.FeatureSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [FeatureSettingsRepository]; everything switched on, like a fresh install. */
class FakeFeatureSettingsRepository : FeatureSettingsRepository {
    override val switchedOn = MutableStateFlow(Feature.entries.filter { it.isSwitchable }.toSet())
    override val enabled = MutableStateFlow(enabledFeatures(switchedOn.value))

    override fun setSwitchedOn(feature: Feature, on: Boolean) {
        if (!feature.isSwitchable) return
        switchedOn.value = if (on) switchedOn.value + feature else switchedOn.value - feature
        enabled.value = enabledFeatures(switchedOn.value)
    }
}
