package dev.malkolm.recipeapp.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations. Each route is a serializable Kotlin type, so arguments
 * (e.g. a recipe id) are checked by the compiler instead of being strings in a URL.
 */
@Serializable
data object RecipeListRoute
