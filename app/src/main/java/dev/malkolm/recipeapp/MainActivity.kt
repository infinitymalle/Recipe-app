package dev.malkolm.recipeapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.malkolm.recipeapp.domain.model.ThemeMode
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import dev.malkolm.recipeapp.ui.components.LocalBackgroundBlur
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
            val backgroundBlur by themeSettingsRepository.backgroundBlur.collectAsState()
            RecipeAppTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalBackgroundBlur provides backgroundBlur.dp) {
                    Box {
                        RecipeNavHost(startDestination = startDestination)
                        StatusBarStrip()
                    }
                }
            }
        }
    }
}

/**
 * A solid band behind the status bar (clock, notifications) so it stands apart from the screen's
 * own top bar instead of blending into it. The icons are switched to dark or light to match it.
 * Drawn on top of every screen: the screens' top bars already leave this space empty.
 */
@Composable
private fun StatusBarStrip() {
    val color = MaterialTheme.colorScheme.primary
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = color.luminance() > 0.5f
        }
    }
    Box(modifier = Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(color))
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
