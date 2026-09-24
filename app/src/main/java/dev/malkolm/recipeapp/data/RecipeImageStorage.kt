package dev.malkolm.recipeapp.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages photo files in the app's private files directory, since the permission a picked
 * `content://` URI grants does not outlive the activity that returned it.
 */
class RecipeImageStorage
@Inject
constructor(@ApplicationContext private val context: Context) {
    /** Where a photo attachment's `filePath` (relative to the files directory) resolves to on disk. */
    fun resolve(relativePath: String): File = File(context.filesDir, relativePath)

    /** Copies a picked photo into private storage. Returns the new file's path, relative to the files directory. */
    suspend fun saveImage(recipeId: String, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val relativePath = newRelativePath(recipeId)
        val destination = resolve(relativePath)
        destination.parentFile?.mkdirs()
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open $sourceUri")
        relativePath
    }

    /**
     * Creates an empty file for the camera app to write a photo into, and a `content://` URI
     * (via [FileProvider]) it is allowed to write to. [CaptureTarget.relativePath] is already the
     * attachment's final path - nothing needs copying once the camera reports success.
     */
    suspend fun createCaptureTarget(recipeId: String): CaptureTarget = withContext(Dispatchers.IO) {
        val relativePath = newRelativePath(recipeId)
        val file = resolve(relativePath)
        file.parentFile?.mkdirs()
        file.createNewFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        CaptureTarget(uri = uri, relativePath = relativePath)
    }

    private fun newRelativePath(recipeId: String) = "recipes/$recipeId/${UUID.randomUUID()}.jpg"

    data class CaptureTarget(val uri: Uri, val relativePath: String)
}
