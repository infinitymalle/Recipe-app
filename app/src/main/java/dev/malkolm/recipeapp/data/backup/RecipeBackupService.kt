package dev.malkolm.recipeapp.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import java.io.File
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
    private val idGenerator: IdGenerator
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Writes every recipe to [destination]. Returns how many were written. */
    suspend fun export(destination: Uri): Int = withContext(Dispatchers.IO) {
        val recipes = recipeRepository.getAllRecipes()
        val output = context.contentResolver.openOutputStream(destination) ?: error("Could not open $destination")
        output.use {
            ZipOutputStream(it).use { zip ->
                val backupRecipes = recipes.map { recipe -> recipe.toBackup(zip) }
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(json.encodeToString(BackupManifest(recipes = backupRecipes)).toByteArray())
                zip.closeEntry()
            }
        }
        recipes.size
    }

    /** Reads every recipe from [source] and saves it. Returns how many were read. */
    suspend fun import(source: Uri): Int = withContext(Dispatchers.IO) {
        val manifest = readManifest(source) ?: error("Backup file has no manifest")
        for (recipe in manifest.recipes) {
            recipeRepository.saveRecipe(recipe.toDraft())
        }
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
     * share sheet) and returns its id, or `null` if [source] has no recipe to import.
     */
    suspend fun importShared(source: Uri): String? = withContext(Dispatchers.IO) {
        val recipe = readManifest(source)?.recipes?.firstOrNull() ?: return@withContext null
        recipeRepository.saveRecipe(recipe.toDraft())
        recipe.id
    }

    private fun readManifest(source: Uri): BackupManifest? {
        val input = context.contentResolver.openInputStream(source) ?: error("Could not open $source")
        return input.use { stream -> ZipInputStream(stream).use { zip -> zip.extractFilesAndReadManifest() } }
    }

    /** Extracts every `files/` entry into place and returns the parsed manifest, if present. */
    private fun ZipInputStream.extractFilesAndReadManifest(): BackupManifest? {
        var manifest: BackupManifest? = null
        var entry = nextEntry
        while (entry != null) {
            when {
                entry.name == MANIFEST_ENTRY -> manifest = json.decodeFromString(readBytes().decodeToString())

                entry.name.startsWith(FILES_PREFIX) -> {
                    val destination = File(context.filesDir, entry.name.removePrefix(FILES_PREFIX))
                    destination.parentFile?.mkdirs()
                    destination.outputStream().use { out -> copyTo(out) }
                }
            }
            closeEntry()
            entry = nextEntry
        }
        return manifest
    }

    private fun Recipe.toBackup(zip: ZipOutputStream): BackupRecipe = BackupRecipe(
        id = id,
        title = title,
        ingredients = ingredients,
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

    /** Adds the file at [relativePath] under `files/` in the zip. Returns [relativePath], or `null` if missing. */
    private fun addFileEntry(zip: ZipOutputStream, relativePath: String): String? {
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
