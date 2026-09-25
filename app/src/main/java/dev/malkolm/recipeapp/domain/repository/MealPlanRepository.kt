package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.PlanEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** The meal plan: which recipe is planned for which meal on which day, and the grocery days. */
interface MealPlanRepository {
    /** Planned entries from [from] to [to], both inclusive, ordered by date. */
    fun observePlan(from: LocalDate, to: LocalDate): Flow<List<PlanEntry>>

    /** Plans [recipeId] for [mealType] on [date], replacing whatever was planned there. */
    suspend fun setMeal(date: LocalDate, mealType: MealType, recipeId: String)

    suspend fun removeMeal(date: LocalDate, mealType: MealType)

    suspend fun setGroceryDay(date: LocalDate, isGroceryDay: Boolean)
}
