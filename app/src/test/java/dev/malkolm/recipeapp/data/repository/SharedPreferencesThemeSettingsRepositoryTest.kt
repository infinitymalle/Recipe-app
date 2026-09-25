package dev.malkolm.recipeapp.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesThemeSettingsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `defaults to system`() {
        val repository = SharedPreferencesThemeSettingsRepository(context)

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.value)
    }

    @Test
    fun `a set theme mode is read back, including by a new instance`() {
        val repository = SharedPreferencesThemeSettingsRepository(context)

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.value)
        assertEquals(ThemeMode.DARK, SharedPreferencesThemeSettingsRepository(context).themeMode.value)
    }

    @Test
    fun `background blur defaults, is kept within range and is read back`() {
        val repository = SharedPreferencesThemeSettingsRepository(context)
        assertEquals(ThemeSettingsRepository.DEFAULT_BLUR, repository.backgroundBlur.value)

        repository.setBackgroundBlur(15)
        assertEquals(15, SharedPreferencesThemeSettingsRepository(context).backgroundBlur.value)

        repository.setBackgroundBlur(500)
        assertEquals(ThemeSettingsRepository.BLUR_RANGE.last, repository.backgroundBlur.value)
    }

    @Test
    fun `the shopping list picture is read back and can be cleared`() {
        val repository = SharedPreferencesThemeSettingsRepository(context)
        assertNull(repository.shoppingListImagePath.value)

        repository.setShoppingListImagePath("backgrounds/cart.jpg")
        assertEquals(
            "backgrounds/cart.jpg",
            SharedPreferencesThemeSettingsRepository(context).shoppingListImagePath.value
        )

        repository.setShoppingListImagePath(null)
        assertNull(SharedPreferencesThemeSettingsRepository(context).shoppingListImagePath.value)
    }
}
