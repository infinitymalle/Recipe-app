package dev.malkolm.recipeapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureTest {
    private val allSwitches = Feature.entries.filter { it.isSwitchable }.toSet()

    @Test
    fun `with every switch on, everything is on`() {
        assertEquals(Feature.entries.toSet(), enabledFeatures(allSwitches))
    }

    @Test
    fun `each feature can be switched off on its own`() {
        for (feature in allSwitches) {
            val enabled = enabledFeatures(allSwitches - feature)

            assertEquals(false, feature in enabled, "$feature")
            // Nothing that does not depend on it is affected.
            for (other in allSwitches - feature) assertEquals(true, other in enabled, "$other with $feature off")
        }
    }

    @Test
    fun `switching off either the planner or the shopping list cascades to the grocery button`() {
        assertEquals(false, Feature.GROCERY_TO_SHOPPING_LIST in enabledFeatures(allSwitches - Feature.MEAL_PLANNER))
        assertEquals(false, Feature.GROCERY_TO_SHOPPING_LIST in enabledFeatures(allSwitches - Feature.SHOPPING_LIST))
        assertEquals(true, Feature.GROCERY_TO_SHOPPING_LIST in enabledFeatures(allSwitches - Feature.COOK_MODE))
    }

    @Test
    fun `with every switch off, only features without a switch whose dependencies are on remain - none`() {
        assertEquals(emptySet(), enabledFeatures(emptySet()))
    }
}
