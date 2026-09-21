package dev.malkolm.recipeapp.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.malkolm.recipeapp.data.local.ALL_MIGRATIONS
import dev.malkolm.recipeapp.data.local.RecipeDatabase
import dev.malkolm.recipeapp.data.local.dao.RecipeDao
import dev.malkolm.recipeapp.data.local.dao.TagDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // One database instance for the whole app: opening several would defeat Room's change
    // notifications and its write locking.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RecipeDatabase = Room
        .databaseBuilder(context, RecipeDatabase::class.java, RecipeDatabase.NAME)
        .addMigrations(*ALL_MIGRATIONS)
        .build()

    @Provides
    fun provideRecipeDao(database: RecipeDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideTagDao(database: RecipeDatabase): TagDao = database.tagDao()
}
