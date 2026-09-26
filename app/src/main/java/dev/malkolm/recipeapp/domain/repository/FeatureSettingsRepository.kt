package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.Feature
import kotlinx.coroutines.flow.StateFlow

/** Which optional features are switched on (see [Feature]). Every feature starts switched on. */
interface FeatureSettingsRepository {
    /** The switches the user has on, before dependencies are applied. */
    val switchedOn: StateFlow<Set<Feature>>

    /** The features that are actually on: [switchedOn] with dependencies applied (`enabledFeatures`). */
    val enabled: StateFlow<Set<Feature>>

    fun setSwitchedOn(feature: Feature, on: Boolean)
}
