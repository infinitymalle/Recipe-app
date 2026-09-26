package dev.malkolm.recipeapp.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.malkolm.recipeapp.R
import dev.malkolm.recipeapp.domain.model.Feature
import dev.malkolm.recipeapp.ui.components.LocalEnabledFeatures
import dev.malkolm.recipeapp.ui.cookrecipe.CookRecipeScreen
import dev.malkolm.recipeapp.ui.importrecipe.ImportRecipeScreen
import dev.malkolm.recipeapp.ui.mealplan.MealPlanScreen
import dev.malkolm.recipeapp.ui.recipedetail.RecipeDetailScreen
import dev.malkolm.recipeapp.ui.recipeedit.RecipeEditScreen
import dev.malkolm.recipeapp.ui.recipelist.RecipeListScreen
import dev.malkolm.recipeapp.ui.settings.SettingsScreen
import dev.malkolm.recipeapp.ui.shoppinglist.ShoppingListScreen

/** The bottom tabs. A tab whose [feature] is switched off is not shown. */
private enum class Tab(val route: Any, val labelRes: Int, val feature: Feature?) {
    RECIPES(RecipeListRoute, R.string.tab_recipes, null),
    PLAN(MealPlanRoute, R.string.tab_plan, Feature.MEAL_PLANNER),
    SHOPPING(ShoppingListRoute, R.string.tab_shopping, Feature.SHOPPING_LIST)
}

@Composable
fun RecipeNavHost(startDestination: Any = RecipeListRoute) {
    val navController = rememberNavController()
    val enabledFeatures = LocalEnabledFeatures.current
    val tabs = Tab.entries.filter { it.feature == null || it.feature in enabledFeatures }
    val destination = navController.currentBackStackEntryAsState().value?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hasRoute(tab.route::class) == true }

    // Fallback: if the tab on screen belongs to a feature that was just switched off, show recipes.
    LaunchedEffect(currentTab, tabs) {
        if (currentTab != null && currentTab !in tabs) {
            navController.navigate(RecipeListRoute) { popUpTo<RecipeListRoute>() }
        }
    }

    Scaffold(
        // The screens inside handle the system bars themselves; this only adds the tab bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null && tabs.size > 1) {
                // Text-only tabs (Material's navigation bar expects an icon per item).
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                    PrimaryTabRow(
                        selectedTabIndex = tabs.indexOf(currentTab).coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = tab == currentTab,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        // Standard tab behaviour: one copy of each tab, each keeping its
                                        // own scroll position, with Recipes at the bottom of the stack.
                                        popUpTo<RecipeListRoute> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                text = {
                                    Text(stringResource(tab.labelRes), style = MaterialTheme.typography.titleSmall)
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.height(56.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        RecipeNavGraph(navController, startDestination, Modifier.padding(padding).consumeWindowInsets(padding))
    }
}

@Composable
private fun RecipeNavGraph(navController: NavHostController, startDestination: Any, modifier: Modifier) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<RecipeListRoute> {
            RecipeListScreen(
                onOpenRecipe = { id -> navController.navigate(RecipeDetailRoute(id)) },
                onAddRecipe = { navController.navigate(RecipeEditRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) }
            )
        }
        composable<ShoppingListRoute> {
            ShoppingListScreen()
        }
        composable<MealPlanRoute> {
            MealPlanScreen(
                onOpenRecipe = { id -> navController.navigate(RecipeDetailRoute(id)) }
            )
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
        composable<ImportRecipeRoute> {
            ImportRecipeScreen(
                onImported = { id ->
                    navController.navigate(RecipeDetailRoute(id)) { popUpTo<ImportRecipeRoute> { inclusive = true } }
                },
                onGiveUp = {
                    if (!navController.popBackStack()) {
                        navController.navigate(RecipeListRoute) { popUpTo(RecipeListRoute) { inclusive = true } }
                    }
                }
            )
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
