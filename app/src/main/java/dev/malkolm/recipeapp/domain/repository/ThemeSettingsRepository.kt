package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** The user's look-and-feel choices, persisted across app restarts. */
interface ThemeSettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)

    /** How strongly background photos are blurred, in dp, within [BLUR_RANGE]. */
    val backgroundBlur: StateFlow<Int>

    fun setBackgroundBlur(dp: Int)

    /** The shopping list's background picture (relative to the files directory), or `null` for none. */
    val shoppingListImagePath: StateFlow<String?>

    fun setShoppingListImagePath(path: String?)

    companion object {
        val BLUR_RANGE = 0..25
        const val DEFAULT_BLUR = 8
    }
}
