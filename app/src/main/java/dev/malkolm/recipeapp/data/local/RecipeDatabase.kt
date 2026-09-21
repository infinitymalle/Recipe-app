package dev.malkolm.recipeapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.malkolm.recipeapp.data.local.dao.RecipeDao
import dev.malkolm.recipeapp.data.local.dao.TagDao
import dev.malkolm.recipeapp.data.local.entity.AttachmentEntity
import dev.malkolm.recipeapp.data.local.entity.RecipeEntity
import dev.malkolm.recipeapp.data.local.entity.RecipeTagCrossRef
import dev.malkolm.recipeapp.data.local.entity.TagEntity

/** The SQLite database. `exportSchema` makes Room write each version's layout to `app/schemas/`. */
@Database(
    entities = [
        RecipeEntity::class,
        AttachmentEntity::class,
        TagEntity::class,
        RecipeTagCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun tagDao(): TagDao

    companion object {
        const val NAME = "recipes.db"
    }
}
