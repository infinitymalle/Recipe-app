package dev.malkolm.recipeapp.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.domain.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
