package dev.malkolm.recipeapp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * [recipeId] is `null` to create a new recipe, or an existing id to edit it.
 *
 * [sharedText] and [sharedImageUri] are only set when this recipe was started from another app's
 * share sheet (see `MainActivity.shareIntentRoute`); the edit screen turns them into a starting
 * link/text/picture attachment.
 */
@Serializable
data class RecipeEditRoute(
    val recipeId: String? = null,
    val sharedText: String? = null,
    val sharedImageUri: String? = null
)
