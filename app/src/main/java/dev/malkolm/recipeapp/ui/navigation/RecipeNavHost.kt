package dev.malkolm.recipeapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.malkolm.recipeapp.ui.recipelist.RecipeListScreen

@Composable
fun RecipeNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = RecipeListRoute) {
        composable<RecipeListRoute> {
            RecipeListScreen()
        }
    }
}
