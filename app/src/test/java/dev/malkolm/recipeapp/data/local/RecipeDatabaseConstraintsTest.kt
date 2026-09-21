package dev.malkolm.recipeapp.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the rules the database itself enforces, independent of the repository code. */
@RunWith(AndroidJUnit4::class)
class RecipeDatabaseConstraintsTest {
    private lateinit var database: RecipeDatabase
    private lateinit var sqlite: SupportSQLiteDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RecipeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sqlite = database.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun count(table: String): Int = sqlite.query("SELECT COUNT(*) FROM $table").use {
        it.moveToFirst()
        it.getInt(0)
    }

    private fun insertRecipe(id: String) = sqlite.execSQL(
        "INSERT INTO recipes (id, title, ingredients, notes, createdAt, updatedAt) VALUES ('$id', 't', '', '', 0, 0)"
    )

    private fun insertAttachment(id: String, recipeId: String) = sqlite.execSQL(
        "INSERT INTO attachments (id, recipeId, type, position) VALUES ('$id', '$recipeId', 'TEXT', 0)"
    )

    @Test
    fun `an attachment cannot point at a recipe that does not exist`() {
        assertFailsWith<SQLiteConstraintException> { insertAttachment("a1", recipeId = "missing") }
    }

    @Test
    fun `removing a recipe row removes its attachments and tag links`() {
        insertRecipe("r1")
        insertAttachment("a1", "r1")
        sqlite.execSQL("INSERT INTO tags (id, name, normalizedName) VALUES ('t1', 'Quick', 'quick')")
        sqlite.execSQL("INSERT INTO recipe_tags (recipeId, tagId) VALUES ('r1', 't1')")

        sqlite.execSQL("DELETE FROM recipes WHERE id = 'r1'")

        assertEquals(0, count("attachments"))
        assertEquals(0, count("recipe_tags"))
        assertEquals(1, count("tags")) // the tag itself may still be used by other recipes
    }

    @Test
    fun `two tags cannot share a normalized name`() {
        sqlite.execSQL("INSERT INTO tags (id, name, normalizedName) VALUES ('t1', 'Dessert', 'dessert')")

        assertFailsWith<SQLiteConstraintException> {
            sqlite.execSQL("INSERT INTO tags (id, name, normalizedName) VALUES ('t2', 'DESSERT', 'dessert')")
        }
    }

    @Test
    fun `a recipe cannot have the same tag twice`() {
        insertRecipe("r1")
        sqlite.execSQL("INSERT INTO tags (id, name, normalizedName) VALUES ('t1', 'Quick', 'quick')")
        sqlite.execSQL("INSERT INTO recipe_tags (recipeId, tagId) VALUES ('r1', 't1')")

        assertFailsWith<SQLiteConstraintException> {
            sqlite.execSQL("INSERT INTO recipe_tags (recipeId, tagId) VALUES ('r1', 't1')")
        }
    }
}
