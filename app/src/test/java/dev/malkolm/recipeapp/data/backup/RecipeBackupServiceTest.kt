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
import kotlin.test.Test
import kotlin.test.assertEquals
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
