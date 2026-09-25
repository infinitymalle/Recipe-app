package dev.malkolm.recipeapp.data.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.testutil.FakeRecipeRepository
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeBackupServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `export then import round-trips a recipe with tags and a photo`() = runTest {
        val sourceRepo = FakeRecipeRepository()
        val exportService = RecipeBackupService(context, sourceRepo, SequentialIdGenerator())

        val imageFile = File(context.filesDir, "recipes/r1/photo.jpg")
        imageFile.parentFile?.mkdirs()
        imageFile.writeBytes(byteArrayOf(1, 2, 3))
        sourceRepo.saveRecipe(
            RecipeDraft(
                id = "r1",
                title = "Pancakes",
                ingredients = "Flour (2 cups)",
                servings = 4,
                tags = listOf(Tag.of("t1", "Breakfast")),
                attachments = listOf(Attachment.Image(id = "a1", filePath = "recipes/r1/photo.jpg"))
            )
        )

        val zipUri = Uri.fromFile(File(context.cacheDir, "backup.zip"))
        assertEquals(1, exportService.export(zipUri))

        val destinationRepo = FakeRecipeRepository()
        val importService = RecipeBackupService(context, destinationRepo, SequentialIdGenerator())
        assertEquals(1, importService.import(zipUri))

        val imported = destinationRepo.observeRecipe("r1").first()
        assertEquals("Pancakes", imported?.title)
        assertEquals(4, imported?.servings)
        assertEquals(listOf("Breakfast"), imported?.tags?.map { it.name })
        val attachment = imported?.attachments?.single() as Attachment.Image
        assertEquals("recipes/r1/photo.jpg", attachment.filePath)
        assertTrue(File(context.filesDir, attachment.filePath).readBytes().contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `a recipe with no attachments still round-trips`() = runTest {
        val sourceRepo = FakeRecipeRepository()
        val service = RecipeBackupService(context, sourceRepo, SequentialIdGenerator())
        sourceRepo.saveRecipe(RecipeDraft(id = "r1", title = "Plain toast"))

        val zipUri = Uri.fromFile(File(context.cacheDir, "backup-plain.zip"))
        service.export(zipUri)

        val destinationRepo = FakeRecipeRepository()
        RecipeBackupService(context, destinationRepo, SequentialIdGenerator()).import(zipUri)

        assertEquals("Plain toast", destinationRepo.observeRecipe("r1").first()?.title)
    }

    @Test
    fun `importShared saves a copy with fresh ids and its photo in the copy's own folder`() = runTest {
        val sourceRepo = FakeRecipeRepository()
        val exportService = RecipeBackupService(context, sourceRepo, SequentialIdGenerator())
        val imageFile = File(context.filesDir, "recipes/r1/photo.jpg")
        imageFile.parentFile?.mkdirs()
        imageFile.writeBytes(byteArrayOf(1, 2, 3))
        sourceRepo.saveRecipe(
            RecipeDraft(
                id = "r1",
                title = "Pancakes",
                ingredients = "Flour (2 cups)",
                attachments = listOf(Attachment.Image(id = "a1", filePath = "recipes/r1/photo.jpg"))
            )
        )

        val sharedFile = exportService.exportForSharing("r1")
        assertNotNull(sharedFile)

        val destinationRepo = FakeRecipeRepository()
        val importService = RecipeBackupService(context, destinationRepo, SequentialIdGenerator("copy"))
        val importedId = importService.importShared(Uri.fromFile(sharedFile))

        assertNotNull(importedId)
        assertNotEquals("r1", importedId)
        val imported = destinationRepo.observeRecipe(importedId).first()
        assertEquals("Pancakes", imported?.title)
        assertEquals("Flour (2 cups)", imported?.ingredients)
        val attachment = imported?.attachments?.single() as Attachment.Image
        assertNotEquals("a1", attachment.id)
        assertEquals("recipes/$importedId/photo.jpg", attachment.filePath)
        assertTrue(File(context.filesDir, attachment.filePath).readBytes().contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `importing a shared recipe back into the phone it came from does not overwrite the original`() = runTest {
        val repo = FakeRecipeRepository()
        val service = RecipeBackupService(context, repo, SequentialIdGenerator())
        val originalPhoto = File(context.filesDir, "recipes/r1/photo.jpg")
        originalPhoto.parentFile?.mkdirs()
        originalPhoto.writeBytes(byteArrayOf(1, 2, 3))
        repo.saveRecipe(
            RecipeDraft(
                id = "r1",
                title = "Pancakes",
                attachments = listOf(Attachment.Image(id = "a1", filePath = "recipes/r1/photo.jpg"))
            )
        )
        val sharedFile = assertNotNull(service.exportForSharing("r1"))

        // Someone edits their copy (title and photo), and shares it back.
        repo.saveRecipe(RecipeDraft(id = "r1", title = "My pancakes"))
        originalPhoto.writeBytes(byteArrayOf(9, 9))

        service.importShared(Uri.fromFile(sharedFile))

        assertEquals(2, repo.getAllRecipes().size)
        assertEquals("My pancakes", repo.observeRecipe("r1").first()?.title)
        assertTrue(originalPhoto.readBytes().contentEquals(byteArrayOf(9, 9)))
    }

    @Test
    fun `a zip entry that points outside the files directory is refused`() = runTest {
        val zipFile = File(context.cacheDir, "evil.recipe.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("files/../evil.txt"))
            zip.write("pwned".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"recipes":[{"id":"r1","title":"Evil","ingredients":""}]}""".toByteArray())
            zip.closeEntry()
        }
        val repo = FakeRecipeRepository()
        val service = RecipeBackupService(context, repo, SequentialIdGenerator())

        assertFailsWith<IllegalArgumentException> { service.import(Uri.fromFile(zipFile)) }
        assertTrue(repo.getAllRecipes().isEmpty())

        // A shared import keeps only each file's name, so the same entry lands in the copy's folder.
        val copyId = service.importShared(Uri.fromFile(zipFile))
        assertTrue(File(context.filesDir, "recipes/$copyId/evil.txt").exists())

        assertFalse(File(context.filesDir.parentFile, "evil.txt").exists())
    }

    @Test
    fun `exportForSharing returns null for a recipe that does not exist`() = runTest {
        val service = RecipeBackupService(context, FakeRecipeRepository(), SequentialIdGenerator())

        assertEquals(null, service.exportForSharing("missing"))
    }

    @Test
    fun `re-importing the same backup updates the recipe in place instead of duplicating it`() = runTest {
        val sourceRepo = FakeRecipeRepository()
        RecipeBackupService(context, sourceRepo, SequentialIdGenerator())
            .also { sourceRepo.saveRecipe(RecipeDraft(id = "r1", title = "Pancakes")) }

        val zipUri = Uri.fromFile(File(context.cacheDir, "backup-reimport.zip"))
        RecipeBackupService(context, sourceRepo, SequentialIdGenerator()).export(zipUri)

        val destinationRepo = FakeRecipeRepository()
        val importService = RecipeBackupService(context, destinationRepo, SequentialIdGenerator())
        importService.import(zipUri)
        importService.import(zipUri)

        assertEquals(1, destinationRepo.getAllRecipes().size)
    }
}
