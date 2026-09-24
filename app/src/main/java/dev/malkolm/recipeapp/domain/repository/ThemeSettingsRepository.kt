package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** The user's chosen light/dark mode, persisted across app restarts. */
interface ThemeSettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)
}
