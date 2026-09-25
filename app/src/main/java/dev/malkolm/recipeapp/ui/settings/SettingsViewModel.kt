package dev.malkolm.recipeapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.data.RecipeImageStorage
import dev.malkolm.recipeapp.data.backup.RecipeBackupService
import dev.malkolm.recipeapp.data.examples.ExampleRecipes
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val backupService: RecipeBackupService,
    private val themeSettingsRepository: ThemeSettingsRepository,
    private val imageStorage: RecipeImageStorage,
    private val recipeRepository: RecipeRepository,
    private val idGenerator: IdGenerator
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themeSettingsRepository.themeMode
    val backgroundBlur: StateFlow<Int> = themeSettingsRepository.backgroundBlur
    val shoppingListImagePath: StateFlow<String?> = themeSettingsRepository.shoppingListImagePath

    fun setThemeMode(mode: ThemeMode) = themeSettingsRepository.setThemeMode(mode)

    fun setBackgroundBlur(dp: Int) = themeSettingsRepository.setBackgroundBlur(dp)

    /** Copies the picked photo into app storage and makes it the shopping list background. */
    suspend fun setShoppingListImage(source: Uri): Result<Unit> = runCatching {
        val newPath = imageStorage.saveBackgroundImage(source)
        val oldPath = shoppingListImagePath.value
        themeSettingsRepository.setShoppingListImagePath(newPath)
        oldPath?.let { imageStorage.delete(it) }
    }

    suspend fun removeShoppingListImage() {
        val oldPath = shoppingListImagePath.value ?: return
        themeSettingsRepository.setShoppingListImagePath(null)
        imageStorage.delete(oldPath)
    }

    /**
     * Adds the built-in example recipes, tagged "Example". They have fixed ids, so adding them
     * again updates (or restores) the same recipes rather than creating duplicates.
     */
    suspend fun addExampleRecipes(): Result<Int> = runCatching {
        val drafts = ExampleRecipes.all(idGenerator::newId)
        drafts.forEach { recipeRepository.saveRecipe(it) }
        drafts.size
    }

    suspend fun exportBackup(destination: Uri): Result<Int> = runCatching { backupService.export(destination) }

    suspend fun importBackup(source: Uri): Result<Int> = runCatching { backupService.import(source) }
}
