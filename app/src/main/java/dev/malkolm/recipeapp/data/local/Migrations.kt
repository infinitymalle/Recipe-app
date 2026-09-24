package dev.malkolm.recipeapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the `shopping_list_items` table (the shopping list feature). No existing data touched. */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `shopping_list_items` (" +
                    "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `amount` TEXT, " +
                    "`isChecked` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
        }
    }

/**
 * Every database migration, in order.
 *
 * When the schema changes: bump `version` in [RecipeDatabase], add a `Migration(old, new)` here
 * (or an `@AutoMigration`), run the tests, and commit the new file in `app/schemas/`.
 * The app never falls back to deleting the database: her recipes must survive every update.
 * See docs/database.md for the step-by-step.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
