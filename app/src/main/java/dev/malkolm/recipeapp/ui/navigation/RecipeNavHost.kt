package dev.malkolm.recipeapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.malkolm.recipeapp.ui.cookrecipe.CookRecipeScreen
import dev.malkolm.recipeapp.ui.recipedetail.RecipeDetailScreen
import dev.malkolm.recipeapp.ui.recipeedit.RecipeEditScreen
import dev.malkolm.recipeapp.ui.recipelist.RecipeListScreen
import dev.malkolm.recipeapp.ui.settings.SettingsScreen
import dev.malkolm.recipeapp.ui.shoppinglist.ShoppingListScreen

@Composable
fun RecipeNavHost(startDestination: Any = RecipeListRoute) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable<RecipeListRoute> {
            RecipeListScreen(
                onOpenRecipe = { id -> navController.navigate(RecipeDetailRoute(id)) },
                onAddRecipe = { navController.navigate(RecipeEditRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenShoppingList = { navController.navigate(ShoppingListRoute) }
            )
        }
        composable<ShoppingListRoute> {
            ShoppingListScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<RecipeDetailRoute> {
            RecipeDetailScreen(
                onBack = { navController.popBackStack() },
                onEditRecipe = { id -> navController.navigate(RecipeEditRoute(id)) },
                onCookRecipe = { id -> navController.navigate(CookRecipeRoute(id)) }
            )
        }
        composable<CookRecipeRoute> {
            CookRecipeScreen(onBack = { navController.popBackStack() })
        }
        composable<RecipeEditRoute> {
            // A share (RecipeEditRoute as the start destination) has no list entry underneath to
            // pop back to, so fall back to opening the list fresh.
            val leaveEditScreen = {
                if (!navController.popBackStack()) {
                    navController.navigate(RecipeListRoute) { popUpTo(RecipeListRoute) { inclusive = true } }
                }
            }
            RecipeEditScreen(onSaved = leaveEditScreen, onCancel = leaveEditScreen)
        }
    }
}
