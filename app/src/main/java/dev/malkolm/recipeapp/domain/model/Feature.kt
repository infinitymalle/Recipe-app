package dev.malkolm.recipeapp.domain.model

/**
 * Optional parts of the app the user can switch off in Settings. Recipes themselves are the core
 * and always on. A feature counts as enabled only when its own switch and every feature in
 * [dependsOn] are on, so switching one off also switches off what is built on it (a cascade).
 *
 * Switching a feature off only hides it; its data is kept, and comes back when it is switched on.
 */
enum class Feature(val dependsOn: Set<Feature> = emptySet()) {
    MEAL_PLANNER,
    SHOPPING_LIST,
    COOK_MODE,

    /**
     * The planner's grocery-day button that fills the shopping list. Not a switch of its own
     * ([isSwitchable] is false): it is on exactly when both features it joins are.
     */
    GROCERY_TO_SHOPPING_LIST(setOf(MEAL_PLANNER, SHOPPING_LIST));

    /** Whether Settings shows a switch for this feature. */
    val isSwitchable: Boolean get() = this != GROCERY_TO_SHOPPING_LIST
}

/**
 * The features that are actually on, given which switches are on ([switchedOn]): a feature is on
 * when its own switch is (always, for one without a switch) and all its dependencies are on.
 */
fun enabledFeatures(switchedOn: Set<Feature>): Set<Feature> {
    fun isOn(feature: Feature): Boolean =
        (!feature.isSwitchable || feature in switchedOn) && feature.dependsOn.all(::isOn)
    return Feature.entries.filter(::isOn).toSet()
}
