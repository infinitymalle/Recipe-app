package dev.malkolm.recipeapp.ui.navigation

import kotlinx.serialization.Serializable

/** [recipeId] is `null` to create a new recipe, or an existing id to edit it. */
@Serializable
data class RecipeEditRoute(val recipeId: String? = null)
