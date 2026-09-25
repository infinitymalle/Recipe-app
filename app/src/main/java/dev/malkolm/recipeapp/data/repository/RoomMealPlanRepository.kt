package dev.malkolm.recipeapp.data.repository

import dev.malkolm.recipeapp.data.local.dao.PlanDao
import dev.malkolm.recipeapp.data.local.dao.PlanEntryRow
import dev.malkolm.recipeapp.data.local.entity.PlanEntryEntity
import dev.malkolm.recipeapp.domain.IdGenerator
import dev.malkolm.recipeapp.domain.model.MealType
import dev.malkolm.recipeapp.domain.model.PlanEntry
import dev.malkolm.recipeapp.domain.repository.MealPlanRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMealPlanRepository
@Inject
constructor(
    private val dao: PlanDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator
) : MealPlanRepository {
    override fun observePlan(from: LocalDate, to: LocalDate): Flow<List<PlanEntry>> =
        dao.observeLive(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun setMeal(date: LocalDate, mealType: MealType, recipeId: String) {
        val existing = dao.findLive(date.toEpochDay(), PlanEntryEntity.KIND_MEAL, mealType.name)
        if (existing != null) {
            if (existing.recipeId != recipeId) dao.setRecipe(existing.id, recipeId, clock.millis())
        } else {
            dao.insert(newEntry(date, PlanEntryEntity.KIND_MEAL, mealType.name, recipeId))
        }
    }

    override suspend fun removeMeal(date: LocalDate, mealType: MealType) {
        dao.findLive(date.toEpochDay(), PlanEntryEntity.KIND_MEAL, mealType.name)
            ?.let { dao.markDeleted(it.id, clock.millis()) }
    }

    override suspend fun setGroceryDay(date: LocalDate, isGroceryDay: Boolean) {
        val existing = dao.findLive(date.toEpochDay(), PlanEntryEntity.KIND_GROCERY, null)
        when {
            isGroceryDay && existing == null ->
                dao.insert(newEntry(date, PlanEntryEntity.KIND_GROCERY, mealType = null, recipeId = null))

            !isGroceryDay && existing != null -> dao.markDeleted(existing.id, clock.millis())
        }
    }

    private fun newEntry(date: LocalDate, kind: String, mealType: String?, recipeId: String?) = PlanEntryEntity(
        id = idGenerator.newId(),
        epochDay = date.toEpochDay(),
        kind = kind,
        mealType = mealType,
        recipeId = recipeId,
        calendarEventId = null,
        updatedAt = clock.millis(),
        deletedAt = null
    )
}

/** `null` for a row this version does not understand (e.g. written by a newer app version). */
internal fun PlanEntryRow.toDomain(): PlanEntry? {
    val date = LocalDate.ofEpochDay(epochDay)
    return when (kind) {
        PlanEntryEntity.KIND_GROCERY -> PlanEntry.GroceryTrip(id = id, date = date)

        PlanEntryEntity.KIND_MEAL -> {
            val type = mealType?.let { name -> MealType.entries.firstOrNull { it.name == name } } ?: return null
            PlanEntry.Meal(
                id = id,
                date = date,
                mealType = type,
                recipeId = recipeId ?: return null,
                recipeTitle = recipeTitle
            )
        }

        else -> null
    }
}
