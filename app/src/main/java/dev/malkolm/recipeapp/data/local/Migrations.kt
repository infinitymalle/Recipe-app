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
 * Gives shopping list items the same change tracking recipes have, ready for a future sync:
 * `updatedAt` (existing rows start at their `createdAt`) and a soft-delete `deletedAt`.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `shopping_list_items` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `shopping_list_items` ADD COLUMN `deletedAt` INTEGER")
            db.execSQL("UPDATE `shopping_list_items` SET `updatedAt` = `createdAt`")
        }
    }

/** Adds the recipe's `method` (its cooking steps), separate from `notes`. Existing rows start empty. */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `recipes` ADD COLUMN `method` TEXT NOT NULL DEFAULT ''")
        }
    }

/** Adds `plan_entries`: the meal planner's planned recipes and grocery days. No existing data touched. */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `plan_entries` (" +
                    "`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `kind` TEXT NOT NULL, " +
                    "`mealType` TEXT, `recipeId` TEXT, `calendarEventId` INTEGER, " +
                    "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_entries_epochDay` ON `plan_entries` (`epochDay`)")
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
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
