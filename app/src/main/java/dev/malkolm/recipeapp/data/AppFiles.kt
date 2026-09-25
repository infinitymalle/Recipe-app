package dev.malkolm.recipeapp.data

import android.net.Uri
import androidx.core.net.toUri

/**
 * Rules for ids, file paths and links that may come from outside the app (a backup or a shared
 * recipe file someone sent, another app's share sheet). Anything failing these is refused rather
 * than trusted, so a crafted file can never make the app read, write or delete its own private
 * files outside the photo folders (e.g. the database), or open a non-web link.
 */
object AppFiles {
    private val safeId = Regex("[A-Za-z0-9_-]{1,100}")

    /** The top-level folders under the files directory that recipes may reference. */
    private val photoFolders = setOf("recipes", "backgrounds")

    /** A recipe id that is safe to use as a folder name: UUIDs and "example-..." ids pass. */
    fun isSafeId(id: String): Boolean = safeId.matches(id)

    /**
     * A relative path inside one of the photo folders, with no way to climb out of it: no "..",
     * no absolute path, no empty or "." segments. E.g. "recipes/<id>/<name>.jpg".
     */
    fun isSafeRelativePath(path: String): Boolean {
        if (path.isEmpty() || path.startsWith("/") || path.contains('\\')) return false
        val segments = path.split('/')
        if (segments.size < 2 || segments.any { it.isEmpty() || it == "." || it == ".." }) return false
        return segments.first() in photoFolders
    }

    /**
     * A file another app handed us (share sheet) that we may read: only `content://` URIs. A
     * `file://` URI could point at our own private files (e.g. the database), which we would then
     * copy with our own permissions (a "confused deputy"). Our own FileProvider's content URIs are
     * fine: it only exposes the photo and share folders (see res/xml/file_paths.xml).
     */
    fun isReadableSharedUri(uri: Uri): Boolean = uri.scheme == "content"

    /** A link that is safe to open from a recipe: only ordinary web pages. */
    fun isWebLink(url: String): Boolean {
        val scheme = url.trim().toUri().scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }
}
