package dev.malkolm.recipeapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import dev.malkolm.recipeapp.ui.navigation.ImportRecipeRoute
import dev.malkolm.recipeapp.ui.navigation.RecipeEditRoute
import dev.malkolm.recipeapp.ui.navigation.RecipeListRoute
import dev.malkolm.recipeapp.ui.navigation.RecipeNavHost
import dev.malkolm.recipeapp.ui.theme.RecipeAppTheme
import javax.inject.Inject

/** The only Activity: hosts all screens as Compose destinations. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themeSettingsRepository: ThemeSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startDestination = shareIntentRoute(intent) ?: RecipeListRoute
        setContent {
            val themeMode by themeSettingsRepository.themeMode.collectAsState()
            val darkTheme =
                when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
            RecipeAppTheme(darkTheme = darkTheme) {
                RecipeNavHost(startDestination = startDestination)
            }
        }
    }
}

/**
 * Turns an incoming share-sheet [Intent] ("Share to Recipe app", see the `SEND` filters in
 * AndroidManifest.xml) into the route to open: the recipe-edit screen pre-filled with a shared
 * link/photo, or (for a `.recipe.zip` made by `RecipeBackupService.exportForSharing`) the
 * import screen. `null` for anything else (a normal launch from the home screen).
 */
fun shareIntentRoute(intent: Intent): Any? {
    if (intent.action != Intent.ACTION_SEND) return null
    val type = intent.type.orEmpty()

    if (type == "text/plain") {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        return if (text.isNullOrBlank()) null else RecipeEditRoute(sharedText = text)
    }

    val streamUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.toString()
        ?: return null
    return when {
        type.startsWith("image/") -> RecipeEditRoute(sharedImageUri = streamUri)
        type == "application/zip" || type == "application/octet-stream" -> ImportRecipeRoute(uri = streamUri)
        else -> null
    }
}
