package dev.malkolm.recipeapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** The clock is injected so `createdAt` / `updatedAt` timestamps can be fixed in tests. */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
