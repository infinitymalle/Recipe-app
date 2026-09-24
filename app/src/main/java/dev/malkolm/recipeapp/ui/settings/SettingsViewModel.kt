package dev.malkolm.recipeapp.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.malkolm.recipeapp.data.backup.RecipeBackupService
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(private val backupService: RecipeBackupService) : ViewModel() {
    suspend fun exportBackup(destination: Uri): Result<Int> = runCatching { backupService.export(destination) }

    suspend fun importBackup(source: Uri): Result<Int> = runCatching { backupService.import(source) }
}
