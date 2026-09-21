package dev.malkolm.recipeapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards against the classic way to corrupt users' databases: changing a table (entity class)
 * without bumping the database `version` and committing the new schema file.
 *
 * Room stamps every database with a hash of its layout. This test compares that hash with the
 * one in the committed file `app/schemas/<database>/<version>.json`. If they differ, the entities
 * changed since that file was written.
 */
@RunWith(AndroidJUnit4::class)
class RecipeDatabaseSchemaTest {
    @Test
    fun `the committed schema file matches the entity classes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database =
            Room
                .inMemoryDatabaseBuilder(context, RecipeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val sqlite = database.openHelper.readableDatabase
            val liveHash =
                sqlite.query("SELECT identity_hash FROM room_master_table WHERE id = 42").use {
                    it.moveToFirst()
                    it.getString(0)
                }

            val schemaFile = "${RecipeDatabase::class.java.name}/${sqlite.version}.json"
            val committedHash =
                context.assets
                    .open(schemaFile)
                    .bufferedReader()
                    .use { JSONObject(it.readText()) }
                    .getJSONObject("database")
                    .getString("identityHash")

            assertEquals(
                committedHash,
                liveHash,
                "Entities changed but $schemaFile was not updated. Bump the database version, " +
                    "add a migration and commit the new schema file (see docs/database.md)."
            )
        } finally {
            database.close()
        }
    }
}
