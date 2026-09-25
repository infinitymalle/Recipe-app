package dev.malkolm.recipeapp.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "settings"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_BACKGROUND_BLUR = "background_blur"
private const val KEY_SHOPPING_LIST_IMAGE = "shopping_list_image"

/**
 * Backed by [android.content.SharedPreferences] rather than Room: it is a handful of bytes read
 * once at startup, not data that benefits from SQL or from surviving a "clear app data" the way
 * recipes should.
 */
@Singleton
class SharedPreferencesThemeSettingsRepository
@Inject
constructor(@ApplicationContext context: Context) :
    ThemeSettingsRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    private val _backgroundBlur =
        MutableStateFlow(prefs.getInt(KEY_BACKGROUND_BLUR, ThemeSettingsRepository.DEFAULT_BLUR))
    override val backgroundBlur: StateFlow<Int> = _backgroundBlur

    override fun setBackgroundBlur(dp: Int) {
        val clamped = dp.coerceIn(ThemeSettingsRepository.BLUR_RANGE)
        prefs.edit { putInt(KEY_BACKGROUND_BLUR, clamped) }
        _backgroundBlur.value = clamped
    }

    private val _shoppingListImagePath = MutableStateFlow(prefs.getString(KEY_SHOPPING_LIST_IMAGE, null))
    override val shoppingListImagePath: StateFlow<String?> = _shoppingListImagePath

    override fun setShoppingListImagePath(path: String?) {
        prefs.edit { putString(KEY_SHOPPING_LIST_IMAGE, path) }
        _shoppingListImagePath.value = path
    }

    private fun readThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }
}
