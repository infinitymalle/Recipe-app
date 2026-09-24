package dev.malkolm.recipeapp.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies a picked photo into the app's private files directory, since the permission the system
 * photo picker grants for its own `content://` URI does not outlive the picking activity.
 */
class RecipeImageStorage
@Inject
constructor(@ApplicationContext private val context: Context) {
    /** Returns the new file's path, relative to the files directory (see [Attachment][dev.malkolm.recipeapp.domain.model.Attachment]). */
    suspend fun saveImage(recipeId: String, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val relativePath = "recipes/$recipeId/${UUID.randomUUID()}.jpg"
        val destination = File(context.filesDir, relativePath)
        destination.parentFile?.mkdirs()
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open $sourceUri")
        relativePath
    }
}
