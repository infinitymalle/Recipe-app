package dev.malkolm.recipeapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.malkolm.recipeapp.data.UuidIdGenerator
import dev.malkolm.recipeapp.data.repository.RoomRecipeRepository
import dev.malkolm.recipeapp.data.repository.RoomTagRepository
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.domain.repository.TagRepository

/**
 * Tells Hilt which implementation to hand out for each domain interface. To add cloud sync later,
 * this is the one place that changes: bind `RecipeRepository` to a syncing implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindRecipeRepository(impl: RoomRecipeRepository): RecipeRepository

    @Binds
    abstract fun bindTagRepository(impl: RoomTagRepository): TagRepository

    @Binds
    abstract fun bindIdGenerator(impl: UuidIdGenerator): IdGenerator
}
