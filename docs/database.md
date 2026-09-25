# Database guide

The app stores everything in one SQLite database (`recipes.db`) through Room. The whole point of
this document: **her recipes must survive every app update.** Room only keeps data safe across
schema changes if we tell it exactly how each old version becomes the next.

## Tables

| Table | Holds |
| --- | --- |
| `recipes` | One row per recipe (title, ingredients text, servings, time, rating, notes, timestamps, `deletedAt`) |
| `attachments` | Photos, links, pasted text, PDFs of a recipe: **paths and text only, never file contents** |
| `tags` | Tag names (unique ignoring case via `normalizedName`) |
| `recipe_tags` | Links recipes to tags (many-to-many) |

Design choices worth knowing:

- **Ids are UUID strings** created on the phone, so records made on two devices can never clash.
  This is what keeps cloud sync possible later.
- **Deleting is soft**: `deletedAt` is set and the row stays, so a delete can be undone and a
  future sync can tell other devices about it.
- **File paths are relative** to the app's files directory, so they stay valid after a backup
  restore.
- **Ingredients are plain text** for now. To make them structured later: add an `ingredients`
  table in a migration, fill it by parsing the old text, keep the text column until you trust it.

## Changing the schema (step by step)

1. Change the entity class(es).
2. Bump `version` in `RecipeDatabase.kt` (e.g. 2 -> 3).
3. Write the migration in `Migrations.kt` and add it to `ALL_MIGRATIONS`, or use Room's
   `autoMigrations` for simple changes (adding a column, adding a table).
4. Build. Room writes `app/schemas/.../<new version>.json`. **Commit it** together with the code.
5. Run `./gradlew testDebugUnitTest`. `RecipeDatabaseSchemaTest` fails if step 2 or 4 was skipped.
6. Write a migration test in `MigrationsTest` (see below). If the schema test fails right after
   the first build, run the tests once more: the new schema file is only picked up on the next run.

Never use `fallbackToDestructiveMigration()`: it silently deletes all recipes on a mismatch.

## Migration tests

Room's `MigrationTestHelper` (in `room-testing`) creates a database at the old version from the
committed schema JSON, inserts sample data, runs the migration and checks the data survived.

With Room 2.8.5 the helper failed under Robolectric (a driver database-name mismatch raised inside
the helper itself), so `MigrationsTest` does the same job by hand and runs in the normal JVM unit
tests: it builds a database file from the committed `<old version>.json`, inserts rows, then opens
it with the real `RecipeDatabase` and `ALL_MIGRATIONS`. Room validates the migrated tables against
the current entities, so a missing column or default fails the test. Add one test per migration.
