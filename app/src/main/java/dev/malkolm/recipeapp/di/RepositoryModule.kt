package dev.malkolm.recipeapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.malkolm.recipeapp.data.UuidIdGenerator
import dev.malkolm.recipeapp.data.backup.AppBackupExtras
import dev.malkolm.recipeapp.data.backup.BackupExtras
import dev.malkolm.recipeapp.data.calendar.AndroidCalendarGateway
import dev.malkolm.recipeapp.data.calendar.CalendarGateway
import dev.malkolm.recipeapp.data.repository.RoomMealPlanRepository
import dev.malkolm.recipeapp.data.repository.RoomRecipeRepository
import dev.malkolm.recipeapp.data.repository.RoomShoppingListRepository
import dev.malkolm.recipeapp.data.repository.RoomTagRepository
import dev.malkolm.recipeapp.data.repository.SharedPreferencesPlannerSettingsRepository
import dev.malkolm.recipeapp.data.repository.SharedPreferencesThemeSettingsRepository
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.repository.MealPlanRepository
import dev.malkolm.recipeapp.domain.repository.PlannerSettingsRepository
import dev.malkolm.recipeapp.domain.repository.RecipeRepository
import dev.malkolm.recipeapp.domain.repository.ShoppingListRepository
import dev.malkolm.recipeapp.domain.repository.TagRepository
import dev.malkolm.recipeapp.domain.repository.ThemeSettingsRepository

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
    abstract fun bindThemeSettingsRepository(impl: SharedPreferencesThemeSettingsRepository): ThemeSettingsRepository

    @Binds
    abstract fun bindShoppingListRepository(impl: RoomShoppingListRepository): ShoppingListRepository

    @Binds
    abstract fun bindIdGenerator(impl: UuidIdGenerator): IdGenerator

    @Binds
    abstract fun bindMealPlanRepository(impl: RoomMealPlanRepository): MealPlanRepository

    @Binds
    abstract fun bindPlannerSettingsRepository(
        impl: SharedPreferencesPlannerSettingsRepository
    ): PlannerSettingsRepository

    @Binds
    abstract fun bindCalendarGateway(impl: AndroidCalendarGateway): CalendarGateway

    @Binds
    abstract fun bindBackupExtras(impl: AppBackupExtras): BackupExtras
}
