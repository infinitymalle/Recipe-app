package dev.malkolm.recipeapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.data.backup.RecipeBackupService
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val backupService: RecipeBackupService,
    private val themeSettingsRepository: ThemeSettingsRepository
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themeSettingsRepository.themeMode

    fun setThemeMode(mode: ThemeMode) = themeSettingsRepository.setThemeMode(mode)

    suspend fun exportBackup(destination: Uri): Result<Int> = runCatching { backupService.export(destination) }

    suspend fun importBackup(source: Uri): Result<Int> = runCatching { backupService.import(source) }
}
