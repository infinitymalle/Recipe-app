package dev.malkolm.recipeapp.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.domain.model.Feature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesFeatureSettingsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a fresh install has every feature on`() {
        assertEquals(Feature.entries.toSet(), SharedPreferencesFeatureSettingsRepository(context).enabled.value)
    }

    @Test
    fun `a switched-off feature stays off after a restart, with its dependents`() {
        SharedPreferencesFeatureSettingsRepository(context).setSwitchedOn(Feature.SHOPPING_LIST, false)

        val reloaded = SharedPreferencesFeatureSettingsRepository(context)
        assertFalse(Feature.SHOPPING_LIST in reloaded.enabled.value)
        assertFalse(Feature.GROCERY_TO_SHOPPING_LIST in reloaded.enabled.value)
        assertTrue(Feature.MEAL_PLANNER in reloaded.enabled.value)

        reloaded.setSwitchedOn(Feature.SHOPPING_LIST, true)
        assertEquals(Feature.entries.toSet(), SharedPreferencesFeatureSettingsRepository(context).enabled.value)
    }
}
