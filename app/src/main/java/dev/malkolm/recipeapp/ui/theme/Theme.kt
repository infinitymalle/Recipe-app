package dev.malkolm.recipeapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Every role here (not just primary/secondary) is a shade of pink, including background and
// surface - those are what Cards, Scaffold and TextFields actually paint, and M3's own defaults
// for them are near-white/near-black, which is what made the app still look white before this.
private val LightColors = lightColorScheme(
    primary = Color(0xFFAD1457),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E3),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFFC2185B),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF0F5),
    onBackground = Color(0xFF3A2327),
    surface = Color(0xFFFFF0F5),
    onSurface = Color(0xFF3A2327),
    surfaceVariant = Color(0xFFF3DDE2),
    onSurfaceVariant = Color(0xFF524347),
    outline = Color(0xFF847377)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7D2954),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFF06292),
    onSecondary = Color(0xFF5E1133),
    background = Color(0xFF201417),
    onBackground = Color(0xFFF0DEE1),
    surface = Color(0xFF201417),
    onSurface = Color(0xFFF0DEE1),
    surfaceVariant = Color(0xFF524347),
    onSurfaceVariant = Color(0xFFD6C1C5),
    outline = Color(0xFF9F8C90),
    // Material's default dark error is a pale salmon that reads as pink next to this palette; a
    // clear red keeps destructive actions (e.g. "Yes" to clearing the shopping list) unmistakable.
    error = Color(0xFFFF5A5F)
)

/**
 * Material 3 theme, in shades of pink. Dynamic color (following the device wallpaper) is off by
 * default so the pink palette actually shows instead of being overridden on Android 12+.
 */
@Composable
fun RecipeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkColors
            }

            else -> {
                LightColors
            }
        }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
