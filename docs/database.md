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
2. Bump `version` in `RecipeDatabase.kt` (1 -> 2).
3. Write the migration in `Migrations.kt` and add it to `ALL_MIGRATIONS`, or use Room's
   `autoMigrations` for simple changes (adding a column, adding a table).
4. Build. Room writes `app/schemas/.../2.json`. **Commit it** together with the code.
5. Run `./gradlew testDebugUnitTest`. `RecipeDatabaseSchemaTest` fails if step 2 or 4 was skipped.
6. Write a migration test (see below).

Never use `fallbackToDestructiveMigration()`: it silently deletes all recipes on a mismatch.

## Migration tests

Room's `MigrationTestHelper` (in `room-testing`) creates a database at the old version from the
committed schema JSON, inserts sample data, runs the migration and checks the data survived.

With Room 2.8.5 the helper failed under Robolectric (a driver database-name mismatch raised inside
the helper itself), so it cannot run in the normal JVM unit tests. When the first migration is
written, put its test in `app/src/androidTest` (it runs on a real phone with
`./gradlew connectedDebugAndroidTest` or from Android Studio). Room's Gradle plugin already adds
`app/schemas` to the androidTest assets.
