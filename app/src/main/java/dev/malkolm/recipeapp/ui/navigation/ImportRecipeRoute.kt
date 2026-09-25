package dev.malkolm.recipeapp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * [uri] is a file received via the share sheet (see `MainActivity.shareIntentRoute`), expected to
 * be a single-recipe export made by `RecipeBackupService.exportForSharing`.
 */
@Serializable
data class ImportRecipeRoute(val uri: String)
