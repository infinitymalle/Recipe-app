package dev.malkolm.recipeapp.domain.model

import java.time.Instant

/** Limits that the database and the UI both rely on. */
object RecipeRules {
    const val MIN_RATING = 1
    const val MAX_RATING = 5
}

private fun requireValidNumbers(servings: Int?, cookingTimeMinutes: Int?, rating: Int?) {
    require(servings == null || servings >= 1) { "servings must be at least 1, was $servings" }
    require(cookingTimeMinutes == null || cookingTimeMinutes >= 0) {
        "cookingTimeMinutes must not be negative, was $cookingTimeMinutes"
    }
    require(rating == null || rating in RecipeRules.MIN_RATING..RecipeRules.MAX_RATING) {
        "rating must be ${RecipeRules.MIN_RATING}..${RecipeRules.MAX_RATING}, was $rating"
    }
}

/**
 * A saved recipe as read from storage.
 *
 * [method] holds the cooking steps, one per paragraph (see `stepsFrom`); [notes] is for the
 * cook's own remarks. Older recipes may still have their steps in [notes] (before [method] existed).
 * [ingredients] is free text for now (one ingredient per line). A structured ingredient list can
 * be added later with a database migration without changing the rest of this class.
 * Numbers are `null` when unknown; [rating] `null` means "not rated yet".
 */
data class Recipe(
    val id: String,
    val title: String,
    val ingredients: String,
    val method: String = "",
    val servings: Int?,
    val cookingTimeMinutes: Int?,
    val rating: Int?,
    val notes: String,
    val tags: List<Tag>,
    val attachments: List<Attachment>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        requireValidNumbers(servings, cookingTimeMinutes, rating)
    }

    /** The editable part of this recipe, e.g. to fill the edit screen. */
    fun toDraft(): RecipeDraft = RecipeDraft(
        id = id,
        title = title,
        ingredients = ingredients,
        method = method,
        servings = servings,
        cookingTimeMinutes = cookingTimeMinutes,
        rating = rating,
        notes = notes,
        tags = tags,
        attachments = attachments
    )
}

/**
 * What the user edits and hands to the repository to save.
 *
 * It has no timestamps because the repository owns them: `createdAt` is set on first save and
 * `updatedAt` on every save. The [id] is chosen by the app when the edit screen opens (see
 * [dev.malkolm.recipeapp.domain.IdGenerator]) so photos can be filed under it before the first save.
 */
data class RecipeDraft(
    val id: String,
    val title: String,
    val ingredients: String = "",
    val method: String = "",
    val servings: Int? = null,
    val cookingTimeMinutes: Int? = null,
    val rating: Int? = null,
    val notes: String = "",
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList()
) {
    init {
        requireValidNumbers(servings, cookingTimeMinutes, rating)
    }
}

/** The small subset of a recipe that the list screen needs, cheap to load for every recipe. */
data class RecipeSummary(
    val id: String,
    val title: String,
    val servings: Int?,
    val cookingTimeMinutes: Int?,
    val rating: Int?,
    /** Relative path of the first photo or link thumbnail, or `null` if the recipe has none. */
    val thumbnailPath: String?,
    val updatedAt: Instant
)
