package dev.malkolm.recipeapp.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import dev.malkolm.recipeapp.data.local.RecipeDatabase
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.testutil.MutableClock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the repository against a real (in-memory) Room database. Robolectric supplies a fake
 * Android environment, so this runs on the computer without a phone or emulator.
 */
@RunWith(AndroidJUnit4::class)
class RoomRecipeRepositoryTest {
    private val start = Instant.parse("2026-01-01T10:00:00Z")

    private lateinit var database: RecipeDatabase
    private lateinit var clock: MutableClock
    private lateinit var repository: RoomRecipeRepository
    private lateinit var tagRepository: RoomTagRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RecipeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        clock = MutableClock(start)
        repository = RoomRecipeRepository(database.recipeDao(), clock)
        tagRepository = RoomTagRepository(database.tagDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun load(id: String) = repository.observeRecipe(id).first()

    @Test
    fun `a saved recipe can be read back with all its fields`() = runTest {
        val draft =
            RecipeDraft(
                id = "r1",
                title = "Pancakes",
                ingredients = "2 eggs\n1 dl flour",
                servings = 4,
                cookingTimeMinutes = 20,
                rating = 5,
                notes = "Double the vanilla",
                tags = listOf(Tag.of("t1", "Breakfast")),
                attachments = listOf(Attachment.Text("a1", "Whisk it", "Method"))
            )

        repository.saveRecipe(draft)

        val recipe = assertNotNull(load("r1"))
        assertEquals(draft, recipe.toDraft())
        assertEquals(start, recipe.createdAt)
        assertEquals(start, recipe.updatedAt)
    }

    @Test
    fun `attachments keep their order and their kind`() = runTest {
        val attachments =
            listOf(
                Attachment.Link("a1", "https://example.com/v", "Video", "thumbs/v.jpg"),
                Attachment.Image("a2", "recipes/r1/photo.jpg"),
                Attachment.Pdf("a3", "recipes/r1/card.pdf", "Card"),
                Attachment.Text("a4", "Bake at 200")
            )

        repository.saveRecipe(RecipeDraft(id = "r1", title = "Bread", attachments = attachments))

        assertEquals(attachments, assertNotNull(load("r1")).attachments)
    }

    @Test
    fun `saving again keeps createdAt and moves updatedAt`() = runTest {
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Soup"))
        clock.advanceBy(Duration.ofHours(1))

        repository.saveRecipe(RecipeDraft(id = "r1", title = "Better soup"))

        val recipe = assertNotNull(load("r1"))
        assertEquals("Better soup", recipe.title)
        assertEquals(start, recipe.createdAt)
        assertEquals(start.plus(Duration.ofHours(1)), recipe.updatedAt)
    }

    @Test
    fun `saving again replaces the attachments and tags`() = runTest {
        val breakfast = Tag.of("t1", "Breakfast")
        val quick = Tag.of("t2", "Quick")
        repository.saveRecipe(
            RecipeDraft(
                id = "r1",
                title = "Toast",
                tags = listOf(breakfast, quick),
                attachments = listOf(Attachment.Text("a1", "old"), Attachment.Text("a2", "older"))
            )
        )

        repository.saveRecipe(
            RecipeDraft(
                id = "r1",
                title = "Toast",
                tags = listOf(quick),
                attachments = listOf(Attachment.Text("a3", "new"))
            )
        )

        val recipe = assertNotNull(load("r1"))
        assertEquals(listOf(quick), recipe.tags)
        assertEquals(listOf<Attachment>(Attachment.Text("a3", "new")), recipe.attachments)
    }

    @Test
    fun `tags with the same name ignoring case are one shared tag`() = runTest {
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Cake", tags = listOf(Tag.of("t1", "Dessert"))))
        repository.saveRecipe(RecipeDraft(id = "r2", title = "Pie", tags = listOf(Tag.of("t2", "dessert"))))

        assertEquals(listOf(Tag("t1", "Dessert")), tagRepository.observeTagsInUse().first())
        assertEquals(listOf(Tag("t1", "Dessert")), assertNotNull(load("r2")).tags)
    }

    @Test
    fun `Swedish letters are matched ignoring case too`() = runTest {
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Paj", tags = listOf(Tag.of("t1", "Äpple"))))
        repository.saveRecipe(RecipeDraft(id = "r2", title = "Kaka", tags = listOf(Tag.of("t2", "äpple"))))

        assertEquals(listOf(Tag("t1", "Äpple")), tagRepository.observeTagsInUse().first())
    }

    @Test
    fun `the same tag twice in one recipe is stored once`() = runTest {
        repository.saveRecipe(
            RecipeDraft(id = "r1", title = "Stew", tags = listOf(Tag.of("t1", "Dinner"), Tag.of("t2", " dinner ")))
        )

        assertEquals(listOf(Tag("t1", "Dinner")), assertNotNull(load("r1")).tags)
    }

    @Test
    fun `summaries list the most recently saved recipe first`() = runTest {
        repository.saveRecipe(RecipeDraft(id = "r1", title = "First"))
        clock.advanceBy(Duration.ofMinutes(1))
        repository.saveRecipe(RecipeDraft(id = "r2", title = "Second"))
        clock.advanceBy(Duration.ofMinutes(1))
        repository.saveRecipe(RecipeDraft(id = "r1", title = "First, edited"))

        val titles = repository.observeRecipeSummaries().first().map { it.title }

        assertEquals(listOf("First, edited", "Second"), titles)
    }

    @Test
    fun `summary thumbnail is the first attachment that has a picture`() = runTest {
        repository.saveRecipe(
            RecipeDraft(
                id = "linkFirst",
                title = "A",
                attachments =
                    listOf(
                        Attachment.Text("a1", "no picture"),
                        Attachment.Link("a2", "https://x.test", thumbnailPath = "thumbs/x.jpg"),
                        Attachment.Image("a3", "img/late.jpg")
                    )
            )
        )
        repository.saveRecipe(
            RecipeDraft(
                id = "imageFirst",
                title = "B",
                attachments = listOf(
                    Attachment.Image("b1", "img/first.jpg"),
                    Attachment.Link("b2", "https://y.test")
                )
            )
        )
        repository.saveRecipe(
            RecipeDraft(
                id = "skipsLinkWithoutThumb",
                title = "C",
                attachments = listOf(Attachment.Link("c1", "https://z.test"), Attachment.Image("c2", "img/c.jpg"))
            )
        )
        repository.saveRecipe(
            RecipeDraft(id = "textOnly", title = "D", attachments = listOf(Attachment.Text("d1", "just words")))
        )

        val thumbnails = repository.observeRecipeSummaries().first().associate { it.id to it.thumbnailPath }

        assertEquals("thumbs/x.jpg", thumbnails["linkFirst"])
        assertEquals("img/first.jpg", thumbnails["imageFirst"])
        assertEquals("img/c.jpg", thumbnails["skipsLinkWithoutThumb"])
        assertNull(thumbnails["textOnly"])
    }

    @Test
    fun `a deleted recipe disappears and restoring brings it back intact`() = runTest {
        val draft =
            RecipeDraft(
                id = "r1",
                title = "Soup",
                tags = listOf(Tag.of("t1", "Quick")),
                attachments = listOf(Attachment.Text("a1", "stir"))
            )
        repository.saveRecipe(draft)

        repository.deleteRecipe("r1")

        assertNull(load("r1"))
        assertEquals(emptyList(), repository.observeRecipeSummaries().first())
        assertEquals(emptyList(), tagRepository.observeTagsInUse().first())

        clock.advanceBy(Duration.ofMinutes(5))
        repository.restoreRecipe("r1")

        assertEquals(draft, assertNotNull(load("r1")).toDraft())
        assertEquals(listOf(Tag("t1", "Quick")), tagRepository.observeTagsInUse().first())
    }

    @Test
    fun `delete and restore touch updatedAt so a sync could see them`() = runTest {
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Soup"))
        clock.advanceBy(Duration.ofMinutes(5))
        repository.deleteRecipe("r1")
        clock.advanceBy(Duration.ofMinutes(5))
        repository.restoreRecipe("r1")

        assertEquals(start.plus(Duration.ofMinutes(10)), assertNotNull(load("r1")).updatedAt)
    }

    @Test
    fun `restoring a recipe that is not deleted changes nothing`() = runTest {
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Soup"))
        clock.advanceBy(Duration.ofMinutes(5))

        repository.restoreRecipe("r1")

        assertEquals(start, assertNotNull(load("r1")).updatedAt)
    }

    @Test
    fun `deleting or restoring an unknown recipe does nothing`() = runTest {
        repository.deleteRecipe("missing")
        repository.restoreRecipe("missing")

        assertNull(load("missing"))
    }

    @Test
    fun `saving a deleted recipe again makes it visible again`() = runTest {
        repository.saveRecipe(RecipeDraft(id = "r1", title = "Soup"))
        repository.deleteRecipe("r1")

        repository.saveRecipe(RecipeDraft(id = "r1", title = "Soup, rewritten"))

        assertEquals("Soup, rewritten", assertNotNull(load("r1")).title)
    }

    @Test
    fun `a failed save leaves nothing behind`() = runTest {
        // Two attachments with the same id violate the primary key halfway through the save.
        val broken =
            RecipeDraft(
                id = "r1",
                title = "Broken",
                tags = listOf(Tag.of("t1", "Oops")),
                attachments = listOf(Attachment.Text("same", "a"), Attachment.Text("same", "b"))
            )

        assertFailsWith<SQLiteConstraintException> { repository.saveRecipe(broken) }

        assertNull(load("r1"))
        assertEquals(emptyList(), tagRepository.observeTagsInUse().first())
        assertEquals(0, database.openHelper.readableDatabase.rowCount("tags"))
    }

    @Test
    fun `an unknown recipe id gives null`() = runTest {
        assertNull(load("nope"))
    }

    @Test
    fun `the summary list updates by itself when a recipe is saved`() = runTest {
        repository.observeRecipeSummaries().test {
            assertEquals(emptyList(), awaitItem())

            repository.saveRecipe(RecipeDraft(id = "r1", title = "Soup"))

            assertEquals(listOf("r1"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.rowCount(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }
}
