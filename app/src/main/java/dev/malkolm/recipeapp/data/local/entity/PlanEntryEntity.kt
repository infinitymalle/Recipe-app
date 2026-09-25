package dev.malkolm.recipeapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in the `plan_entries` table: a recipe planned for a meal on a day ([kind] "MEAL", with
 * [mealType] and [recipeId]), or a grocery shopping day ([kind] "GROCERY").
 *
 * [epochDay] is the date as days since 1970-01-01 (`LocalDate.toEpochDay`), so it sorts and
 * compares as a plain number. [calendarEventId] is the phone-calendar event made for this entry,
 * if any (see MealPlanCalendarSync). Like recipes and shopping items, rows are soft-deleted
 * ([deletedAt]) and carry [updatedAt], ready for a future sync.
 */
@Entity(tableName = "plan_entries", indices = [Index("epochDay")])
data class PlanEntryEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val kind: String,
    val mealType: String?,
    val recipeId: String?,
    val calendarEventId: Long?,
    val updatedAt: Long,
    val deletedAt: Long?
) {
    companion object {
        const val KIND_MEAL = "MEAL"
        const val KIND_GROCERY = "GROCERY"
    }
}
