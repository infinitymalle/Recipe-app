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
 * AndroidManifest.xml) into the recipe-edit route to open, pre-filled with what was shared.
 * `null` for anything else (a normal launch from the home screen).
 */
fun shareIntentRoute(intent: Intent): RecipeEditRoute? {
    if (intent.action != Intent.ACTION_SEND) return null
    val type = intent.type.orEmpty()
    val sharedImageUri =
        if (type.startsWith("image/")) {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.toString()
        } else {
            null
        }
    val sharedText = if (type == "text/plain") intent.getStringExtra(Intent.EXTRA_TEXT) else null
    if (sharedImageUri == null && sharedText.isNullOrBlank()) return null
    return RecipeEditRoute(sharedText = sharedText, sharedImageUri = sharedImageUri)
}
