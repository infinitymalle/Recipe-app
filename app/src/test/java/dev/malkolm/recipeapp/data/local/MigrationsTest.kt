package dev.malkolm.recipeapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration tests that run on the computer. Room's `MigrationTestHelper` fails under Robolectric
 * (see docs/database.md), so this does its job by hand: build a database file exactly as the
 * committed `app/schemas/.../<version>.json` describes, add rows, then open it with the real
 * [RecipeDatabase]. Room runs [ALL_MIGRATIONS] and then checks the result matches the current
 * entities, so a migration that forgets a column (or a default) fails here.
 */
@RunWith(AndroidJUnit4::class)
class MigrationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `2 to 3 keeps shopping list items and starts updatedAt at createdAt`() = runTest {
        createDatabaseAtVersion(2) { db ->
            db.execSQL(
                "INSERT INTO shopping_list_items (id, name, amount, isChecked, createdAt) " +
                    "VALUES ('s1', 'Milk', '1 l', 0, 1000), ('s2', 'Eggs', NULL, 1, 2000)"
            )
        }

        val database =
            Room
                .databaseBuilder(context, RecipeDatabase::class.java, dbName)
                .addMigrations(*ALL_MIGRATIONS)
                .allowMainThreadQueries()
                .build()
        try {
            val items = database.shoppingListDao().observeItems().first()
            assertEquals(listOf("Milk", "Eggs"), items.map { it.name })
            assertEquals(listOf(1000L, 2000L), items.map { it.updatedAt })
            assertEquals(listOf(null, null), items.map { it.deletedAt })
        } finally {
            database.close()
        }
    }

    /** Creates [dbName] with the tables of committed schema [version], then lets [fill] add rows. */
    private fun createDatabaseAtVersion(version: Int, fill: (SupportSQLiteDatabase) -> Unit) {
        val schema =
            context.assets
                .open("${RecipeDatabase::class.java.name}/$version.json")
                .bufferedReader()
                .use { JSONObject(it.readText()) }
                .getJSONObject("database")
        val callback =
            object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        val table = entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = entity.optJSONArray("indices") ?: continue
                        for (j in 0 until indices.length()) {
                            db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                        }
                    }
                    val setupQueries = schema.getJSONArray("setupQueries")
                    for (i in 0 until setupQueries.length()) db.execSQL(setupQueries.getString(i))
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context).name(dbName).callback(callback).build()
            )
        try {
            fill(helper.writableDatabase)
        } finally {
            helper.close()
        }
    }
}
