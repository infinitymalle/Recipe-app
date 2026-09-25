package dev.malkolm.recipeapp.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.malkolm.recipeapp.data.AppFiles
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MANIFEST_ENTRY = "manifest.json"
private const val FILES_PREFIX = "files/"

/** Upper bounds for reading a zip from outside the app; far above any real backup's needs. */
internal object ZipLimits {
    const val MAX_ENTRIES = 10_000
    const val MAX_MANIFEST_BYTES = 20L * 1024 * 1024
    const val MAX_FILE_BYTES = 100L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 4L * 1024 * 1024 * 1024
}

/**
 * Exports every recipe to a single zip file (a [BackupManifest] as `manifest.json`, plus a copy
 * of every photo/PDF attachment under `files/`), and restores one written by this or an earlier
 * version of the app. Recipe, tag and attachment ids are kept as they were on export, so
 * re-importing the same file updates the same recipes in place rather than duplicating them
 * (`saveRecipe` upserts by id; tags additionally merge by name, see [RecipeRepository]).
 */
class RecipeBackupService
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val recipeRepository: RecipeRepository,
    private val idGenerator: IdGenerator,
    /** The meal plan and settings in a full backup; see [AppBackupExtras]. */
    private val extras: BackupExtras = BackupExtras.None
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Writes every recipe, the meal plan and the settings to [destination]. Returns how many recipes were written. */
    suspend fun export(destination: Uri): Int = withContext(Dispatchers.IO) {
        val recipes = recipeRepository.getAllRecipes()
        val output = context.contentResolver.openOutputStream(destination) ?: error("Could not open $destination")
        output.use {
            ZipOutputStream(it).use { zip ->
                val backupRecipes = recipes.map { recipe -> recipe.toBackup(zip) }
                val extrasData = extras.export(addFile = { path -> addFileEntry(zip, path) })
                val manifest =
                    BackupManifest(
                        recipes = backupRecipes,
                        mealPlan = extrasData.mealPlan,
                        settings = extrasData.settings
                    )
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(json.encodeToString(manifest).toByteArray())
                zip.closeEntry()
            }
        }
        recipes.size
    }

    /** Restores every recipe, the meal plan and the settings from [source]. Returns how many recipes were read. */
    suspend fun import(source: Uri): Int = withContext(Dispatchers.IO) {
        val manifest = readManifest(source) ?: error("Backup file has no manifest")
        // Checked before anything is saved, so a crafted file changes nothing.
        manifest.recipes.forEach { it.requireSafe() }
        for (recipe in manifest.recipes) {
            recipeRepository.saveRecipe(recipe.toDraft())
        }
        extras.restore(BackupExtrasData(manifest.mealPlan, manifest.settings))
        manifest.recipes.size
    }

    /**
     * Writes one recipe to a cache file, ready to be shared. Same on-disk format as [export],
     * just with a single recipe, so [import] or [importShared] can read it back. The caller turns
     * this into a `content://` [Uri] (via `FileProvider`) to put in an `ACTION_SEND` intent - kept
     * out of this class since `FileProvider` needs a real Android provider registry to resolve,
     * which isn't available under Robolectric's test environment.
     */
    suspend fun exportForSharing(recipeId: String): File? = withContext(Dispatchers.IO) {
        val recipe = recipeRepository.observeRecipe(recipeId).first() ?: return@withContext null
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "${recipe.title.toFileSlug()}.recipe.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            val backupRecipe = recipe.toBackup(zip)
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(json.encodeToString(BackupManifest(recipes = listOf(backupRecipe))).toByteArray())
            zip.closeEntry()
        }
        file
    }

    /**
     * Imports the first recipe from [source] (a file made by [exportForSharing], received via the
     * share sheet) as a new copy and returns the copy's id, or `null` if [source] has no recipe.
     *
     * Unlike [import], every id is replaced with a fresh one: a shared recipe comes from someone
     * else, so keeping its id would let it silently overwrite a recipe already here (e.g. one you
     * sent them that they edited and sent back).
     */
    suspend fun importShared(source: Uri): String? = withContext(Dispatchers.IO) {
        val newId = idGenerator.newId()
        // The photos move too, into the copy's own folder, so they can never overwrite the files
        // of the recipe they were copied from.
        val movePath = { path: String -> "recipes/$newId/${File(path).name}" }
        val recipe = readManifest(source, movePath)?.recipes?.firstOrNull() ?: return@withContext null
        val copy = recipe.asNewCopy(newId, movePath)
        copy.requireSafe()
        recipeRepository.saveRecipe(copy.toDraft())
        copy.id
    }

    /**
     * Refuses a recipe read from a file whose id or file paths could reach outside the photo
     * folders: an id like "../x" becomes a folder name, and a photo path like "../databases/..."
     * would be read (and put into the next share file) or deleted later. See [AppFiles].
     */
    private fun BackupRecipe.requireSafe() {
        require(AppFiles.isSafeId(id)) { "Unsafe recipe id in file" }
        val paths =
            attachments.flatMap { attachment ->
                when (attachment) {
                    is BackupAttachment.Text -> emptyList()
                    is BackupAttachment.Link -> listOfNotNull(attachment.thumbnailPath)
                    is BackupAttachment.Image -> listOf(attachment.filePath)
                    is BackupAttachment.Pdf -> listOfNotNull(attachment.filePath, attachment.thumbnailPath)
                }
            }
        require(paths.all(AppFiles::isSafeRelativePath)) { "Unsafe file path in file" }
    }

    private fun BackupRecipe.asNewCopy(newId: String, movePath: (String) -> String): BackupRecipe = copy(
        id = newId,
        attachments =
            attachments.map { attachment ->
                when (attachment) {
                    is BackupAttachment.Text -> attachment.copy(id = idGenerator.newId())

                    is BackupAttachment.Link ->
                        attachment.copy(
                            id = idGenerator.newId(),
                            thumbnailPath = attachment.thumbnailPath?.let(movePath)
                        )

                    is BackupAttachment.Image ->
                        attachment.copy(id = idGenerator.newId(), filePath = movePath(attachment.filePath))

                    is BackupAttachment.Pdf ->
                        attachment.copy(
                            id = idGenerator.newId(),
                            filePath = movePath(attachment.filePath),
                            thumbnailPath = attachment.thumbnailPath?.let(movePath)
                        )
                }
            }
    )

    /** [mapPath] turns each file's path in the zip into where it is saved (unchanged by default). */
    private fun readManifest(source: Uri, mapPath: (String) -> String = { it }): BackupManifest? {
        val input = context.contentResolver.openInputStream(source) ?: error("Could not open $source")
        return input.use { stream -> ZipInputStream(stream).use { zip -> zip.extractFilesAndReadManifest(mapPath) } }
    }

    /**
     * Extracts every `files/` entry into place and returns the parsed manifest, if present.
     * Sizes are capped (see [ZipLimits]) so a "zip bomb" cannot fill the phone or run it out of
     * memory; the sizes a zip claims are not trusted, the bytes are counted while copying.
     */
    private fun ZipInputStream.extractFilesAndReadManifest(mapPath: (String) -> String): BackupManifest? {
        var manifest: BackupManifest? = null
        var entryCount = 0
        var totalBytes = 0L
        var entry = nextEntry
        while (entry != null) {
            require(++entryCount <= ZipLimits.MAX_ENTRIES) { "Too many files in zip" }
            when {
                entry.name == MANIFEST_ENTRY -> {
                    val bytes = ByteArrayOutputStream()
                    copyLimited(bytes, ZipLimits.MAX_MANIFEST_BYTES)
                    manifest = json.decodeFromString(bytes.toByteArray().decodeToString())
                }

                entry.name.startsWith(FILES_PREFIX) && !entry.isDirectory -> {
                    val relativePath = mapPath(entry.name.removePrefix(FILES_PREFIX))
                    require(AppFiles.isSafeRelativePath(relativePath)) { "Unsafe file path in zip" }
                    val destination = safeDestination(relativePath)
                    destination.parentFile?.mkdirs()
                    val limit = minOf(ZipLimits.MAX_FILE_BYTES, ZipLimits.MAX_TOTAL_BYTES - totalBytes)
                    totalBytes += destination.outputStream().use { out -> copyLimited(out, limit) }
                }
            }
            closeEntry()
            entry = nextEntry
        }
        return manifest
    }

    /** Copies the current entry, failing once more than [limit] bytes have been read. */
    private fun ZipInputStream.copyLimited(out: OutputStream, limit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) return copied
            copied += read
            require(copied <= limit) { "File in zip is too large" }
            out.write(buffer, 0, read)
        }
    }

    private fun Recipe.toBackup(zip: ZipOutputStream): BackupRecipe = BackupRecipe(
        id = id,
        title = title,
        ingredients = ingredients,
        method = method,
        servings = servings,
        cookingTimeMinutes = cookingTimeMinutes,
        rating = rating,
        notes = notes,
        tags = tags.map { it.name },
        attachments = attachments.mapNotNull { it.toBackup(zip) }
    )

    /** `null` if a referenced file is missing on disk, so a stale attachment does not break the export. */
    private fun Attachment.toBackup(zip: ZipOutputStream): BackupAttachment? = when (this) {
        is Attachment.Text -> BackupAttachment.Text(id = id, text = text, title = title)

        is Attachment.Link ->
            BackupAttachment.Link(
                id = id,
                url = url,
                title = title,
                thumbnailPath = thumbnailPath?.let { addFileEntry(zip, it) }
            )

        is Attachment.Image -> {
            val path = addFileEntry(zip, filePath) ?: return null
            BackupAttachment.Image(id = id, filePath = path, title = title)
        }

        is Attachment.Pdf -> {
            val path = addFileEntry(zip, filePath) ?: return null
            BackupAttachment.Pdf(
                id = id,
                filePath = path,
                title = title,
                thumbnailPath = thumbnailPath?.let { addFileEntry(zip, it) }
            )
        }
    }

    /**
     * Where a zip entry's file goes, refusing any path that escapes the files directory ("Zip
     * Slip": an entry named `files/../databases/recipes.db` would otherwise overwrite the database).
     * Shared files come from other people, so the zip cannot be trusted.
     */
    private fun safeDestination(relativePath: String): File {
        val root = context.filesDir.canonicalFile
        val destination = File(root, relativePath).canonicalFile
        require(destination.toPath().startsWith(root.toPath()) && destination != root) {
            "Refusing zip entry outside the files directory: $relativePath"
        }
        return destination
    }

    /** Adds the file at [relativePath] under `files/` in the zip. Returns [relativePath], or `null` if missing. */
    private fun addFileEntry(zip: ZipOutputStream, relativePath: String): String? {
        // Never put anything but photos into a file that leaves the phone, whatever the path says.
        if (!AppFiles.isSafeRelativePath(relativePath)) return null
        val file = File(context.filesDir, relativePath)
        if (!file.isFile) return null
        zip.putNextEntry(ZipEntry(FILES_PREFIX + relativePath))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
        return relativePath
    }

    private fun BackupRecipe.toDraft(): RecipeDraft = RecipeDraft(
        id = id,
        title = title,
        ingredients = ingredients,
        method = method,
        servings = servings,
        cookingTimeMinutes = cookingTimeMinutes,
        rating = rating,
        notes = notes,
        tags = tags.map { name -> Tag.of(idGenerator.newId(), name) },
        attachments = attachments.map { it.toDomain() }
    )

    private fun BackupAttachment.toDomain(): Attachment = when (this) {
        is BackupAttachment.Text -> Attachment.Text(id = id, text = text, title = title)

        is BackupAttachment.Link -> Attachment.Link(id = id, url = url, title = title, thumbnailPath = thumbnailPath)

        is BackupAttachment.Image -> Attachment.Image(id = id, filePath = filePath, title = title)

        is BackupAttachment.Pdf ->
            Attachment.Pdf(id = id, filePath = filePath, title = title, thumbnailPath = thumbnailPath)
    }
}

/** A safe file basename from a recipe title, e.g. "Mom's Pancakes!" -> "Moms-Pancakes". */
private fun String.toFileSlug(): String {
    val slug = trim().replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')
    return slug.ifEmpty { "recipe" }
}
