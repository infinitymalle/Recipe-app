package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ThemeSettingsRepository] for ViewModel tests. */
class FakeThemeSettingsRepository : ThemeSettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val backgroundBlur = MutableStateFlow(ThemeSettingsRepository.DEFAULT_BLUR)
    override val shoppingListImagePath = MutableStateFlow<String?>(null)

    override fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override fun setBackgroundBlur(dp: Int) {
        backgroundBlur.value = dp.coerceIn(ThemeSettingsRepository.BLUR_RANGE)
    }

    override fun setShoppingListImagePath(path: String?) {
        shoppingListImagePath.value = path
    }
}
