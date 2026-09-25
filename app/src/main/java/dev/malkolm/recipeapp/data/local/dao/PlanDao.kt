package dev.malkolm.recipeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import dev.malkolm.recipeapp.data.local.entity.PlanEntryEntity
import kotlinx.coroutines.flow.Flow

/** A plan entry with what the calendar event and the screen need from its recipe. */
data class PlanEntryRow(
    val id: String,
    val epochDay: Long,
    val kind: String,
    val mealType: String?,
    val recipeId: String?,
    val calendarEventId: Long?,
    val deletedAt: Long?,
    /** `null` when there is no recipe, or it was deleted. */
    val recipeTitle: String?,
    val recipeIngredients: String?
)

@Dao
interface PlanDao {
    @Query(
        """
        SELECT p.id, p.epochDay, p.kind, p.mealType, p.recipeId, p.calendarEventId, p.deletedAt,
               r.title AS recipeTitle, r.ingredients AS recipeIngredients
          FROM plan_entries p
          LEFT JOIN recipes r ON r.id = p.recipeId AND r.deletedAt IS NULL
         WHERE p.deletedAt IS NULL AND p.epochDay BETWEEN :fromDay AND :toDay
         ORDER BY p.epochDay
        """
    )
    fun observeLive(fromDay: Long, toDay: Long): Flow<List<PlanEntryRow>>

    /** Every entry from [fromDay] on, deleted ones included: the calendar sync removes their events. */
    @Query(
        """
        SELECT p.id, p.epochDay, p.kind, p.mealType, p.recipeId, p.calendarEventId, p.deletedAt,
               r.title AS recipeTitle, r.ingredients AS recipeIngredients
          FROM plan_entries p
          LEFT JOIN recipes r ON r.id = p.recipeId AND r.deletedAt IS NULL
         WHERE p.epochDay >= :fromDay
        """
    )
    suspend fun getAllFrom(fromDay: Long): List<PlanEntryRow>

    @Query(
        """
        SELECT * FROM plan_entries
         WHERE deletedAt IS NULL AND epochDay = :epochDay AND kind = :kind
           AND (mealType = :mealType OR (mealType IS NULL AND :mealType IS NULL))
        """
    )
    suspend fun findLive(epochDay: Long, kind: String, mealType: String?): PlanEntryEntity?

    @Insert
    suspend fun insert(entry: PlanEntryEntity)

    @Upsert
    suspend fun upsert(entry: PlanEntryEntity)

    @Query("SELECT * FROM plan_entries WHERE deletedAt IS NULL ORDER BY epochDay")
    suspend fun getAllLive(): List<PlanEntryEntity>

    @Query("SELECT * FROM plan_entries WHERE id = :id")
    suspend fun getById(id: String): PlanEntryEntity?

    @Query("UPDATE plan_entries SET recipeId = :recipeId, updatedAt = :at WHERE id = :id")
    suspend fun setRecipe(id: String, recipeId: String, at: Long)

    @Query("UPDATE plan_entries SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun markDeleted(id: String, at: Long)

    /** Not a user edit, so [PlanEntryEntity.updatedAt] is left alone. */
    @Query("UPDATE plan_entries SET calendarEventId = :eventId WHERE id = :id")
    suspend fun setCalendarEventId(id: String, eventId: Long?)
}
