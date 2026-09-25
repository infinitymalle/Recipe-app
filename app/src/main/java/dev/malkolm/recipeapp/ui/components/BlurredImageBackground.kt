package dev.malkolm.recipeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository
import java.io.File

/** How strongly [BlurredImageBackground] blurs, from the user's setting (provided in MainActivity). */
val LocalBackgroundBlur = compositionLocalOf { ThemeSettingsRepository.DEFAULT_BLUR.dp }

/**
 * A recipe photo filling the screen behind [content], softly blurred and dimmed so text on top
 * stays readable. [imagePath] is relative to the files directory; `null` shows [content] alone.
 * Screens using this should give their `Scaffold` a transparent `containerColor` when
 * [imagePath] is set, or the Scaffold paints over the photo.
 */
@Composable
fun BlurredImageBackground(
    imagePath: String?,
    modifier: Modifier = Modifier,
    blur: Dp = LocalBackgroundBlur.current,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (imagePath != null) {
            val context = LocalContext.current
            AsyncImage(
                model = File(context.filesDir, imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(blur)
            )
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)))
        }
        content()
    }
}
