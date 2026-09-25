package dev.malkolm.recipeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

/**
 * A recipe photo filling the screen behind [content], softly blurred and dimmed so text on top
 * stays readable. [imagePath] is relative to the files directory; `null` shows [content] alone.
 * Screens using this should give their `Scaffold` a transparent `containerColor` when
 * [imagePath] is set, or the Scaffold paints over the photo.
 */
@Composable
fun BlurredImageBackground(imagePath: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        if (imagePath != null) {
            val context = LocalContext.current
            AsyncImage(
                model = File(context.filesDir, imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(8.dp)
            )
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)))
        }
        content()
    }
}
