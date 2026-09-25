package dev.malkolm.recipeapp

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.ui.navigation.ImportRecipeRoute
import dev.malkolm.recipeapp.ui.navigation.RecipeEditRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun `a normal launch is not a share`() {
        assertNull(shareIntentRoute(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun `sharing a link opens the edit screen with it as the shared text`() {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://example.com/pancakes")
            }

        assertEquals(
            RecipeEditRoute(sharedText = "https://example.com/pancakes"),
            shareIntentRoute(intent)
        )
    }

    @Test
    fun `sharing a photo opens the edit screen with it as the shared image`() {
        val uri = Uri.parse("content://media/external/images/1")
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
            }

        assertEquals(RecipeEditRoute(sharedImageUri = uri.toString()), shareIntentRoute(intent))
    }

    @Test
    fun `a blank shared text is not treated as a share`() {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "   ")
            }

        assertNull(shareIntentRoute(intent))
    }

    @Test
    fun `sharing a recipe zip opens the import screen with it`() {
        val uri = Uri.parse("content://dev.malkolm.recipeapp.fileprovider/shared/pancakes.recipe.zip")
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
            }

        assertEquals(ImportRecipeRoute(uri = uri.toString()), shareIntentRoute(intent))
    }

    @Test
    fun `sharing a recipe file labeled as octet-stream also opens the import screen`() {
        val uri = Uri.parse("content://dev.malkolm.recipeapp.fileprovider/shared/pancakes.recipe.zip")
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
            }

        assertEquals(ImportRecipeRoute(uri = uri.toString()), shareIntentRoute(intent))
    }

    @Test
    fun `a shared file URI is ignored, since it could point at the app's own private files`() {
        val database = Uri.parse("file:///data/data/dev.malkolm.recipeapp/databases/recipes.db")
        for (type in listOf("image/jpeg", "application/zip")) {
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    this.type = type
                    putExtra(Intent.EXTRA_STREAM, database)
                }

            assertNull(shareIntentRoute(intent), type)
        }
    }
}
