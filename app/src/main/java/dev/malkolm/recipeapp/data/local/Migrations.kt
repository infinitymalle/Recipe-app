package dev.malkolm.recipeapp.data.local

import androidx.room.migration.Migration

/**
 * Every database migration, in order. There are none yet because version 1 is the first version.
 *
 * When the schema changes: bump `version` in [RecipeDatabase], add a `Migration(old, new)` here
 * (or an `@AutoMigration`), run the tests, and commit the new file in `app/schemas/`.
 * The app never falls back to deleting the database: her recipes must survive every update.
 * See docs/database.md for the step-by-step.
 */
val ALL_MIGRATIONS: Array<Migration> = emptyArray()
