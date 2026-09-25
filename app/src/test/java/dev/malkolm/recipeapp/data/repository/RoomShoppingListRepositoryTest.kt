package dev.malkolm.recipeapp.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.malkolm.recipeapp.data.local.RecipeDatabase
import dev.malkolm.recipeapp.testutil.MutableClock
import dev.malkolm.recipeapp.testutil.SequentialIdGenerator
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real (in-memory) Room database, including the rows hidden by soft delete. */
@RunWith(AndroidJUnit4::class)
class RoomShoppingListRepositoryTest {
    private val start = Instant.parse("2026-01-01T10:00:00Z")

    private lateinit var database: RecipeDatabase
    private lateinit var clock: MutableClock
    private lateinit var repository: RoomShoppingListRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RecipeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        clock = MutableClock(start)
        repository = RoomShoppingListRepository(database.shoppingListDao(), clock, SequentialIdGenerator())
    }

    @After
    fun tearDown() = database.close()

    private data class Row(val id: String, val updatedAt: Long, val deletedAt: Long?)

    /** Every row, deleted ones included, which the DAO's own reads never return. */
    private fun allRows(): List<Row> = database.openHelper.readableDatabase
        .query("SELECT id, updatedAt, deletedAt FROM shopping_list_items")
        .use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Row(
                            id = cursor.getString(0),
                            updatedAt = cursor.getLong(1),
                            deletedAt = if (cursor.isNull(2)) null else cursor.getLong(2)
                        )
                    )
                }
            }
        }

    @Test
    fun `adding, merging and checking an item each stamp updatedAt`() = runTest {
        repository.addItem("Milk", "1 l")
        assertEquals(start.toEpochMilli(), allRows().single().updatedAt)

        clock.advanceBy(Duration.ofMinutes(1))
        repository.addItem("milk", "1 l")
        assertEquals(start.plusSeconds(60).toEpochMilli(), allRows().single().updatedAt)

        clock.advanceBy(Duration.ofMinutes(1))
        repository.setChecked(allRows().single().id, true)
        assertEquals(start.plusSeconds(120).toEpochMilli(), allRows().single().updatedAt)
    }

    @Test
    fun `deleting an item hides it but keeps the row with deletedAt set`() = runTest {
        repository.addItem("Milk", null)
        repository.addItem("Eggs", null)
        val milk = repository.observeItems().first().single { it.name == "Milk" }

        clock.advanceBy(Duration.ofMinutes(5))
        repository.deleteItem(milk.id)

        assertEquals(listOf("Eggs"), repository.observeItems().first().map { it.name })
        val deletedRow = allRows().single { it.id == milk.id }
        val deletedAt = start.plus(Duration.ofMinutes(5)).toEpochMilli()
        assertEquals(deletedAt, deletedRow.deletedAt)
        assertEquals(deletedAt, deletedRow.updatedAt)
    }

    @Test
    fun `clearing checked items soft-deletes only the checked ones`() = runTest {
        repository.addItem("Milk", null)
        repository.addItem("Eggs", null)
        val eggs = repository.observeItems().first().single { it.name == "Eggs" }
        repository.setChecked(eggs.id, true)

        repository.clearChecked()

        assertEquals(listOf("Milk"), repository.observeItems().first().map { it.name })
        assertEquals(2, allRows().size)
        assertNull(allRows().single { it.id != eggs.id }.deletedAt)
    }

    @Test
    fun `a deleted item does not absorb a newly added item with the same name`() = runTest {
        repository.addItem("Milk", "1 l")
        repository.deleteItem(repository.observeItems().first().single().id)

        repository.addItem("Milk", "2 l")

        assertEquals(listOf("2 l"), repository.observeItems().first().map { it.amount })
    }
}
