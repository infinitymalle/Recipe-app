package dev.malkolm.recipeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.malkolm.recipeapp.ui.navigation.RecipeNavHost
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme

/** The only Activity: hosts all screens as Compose destinations. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeAppTheme {
                RecipeNavHost()
            }
        }
    }
}
