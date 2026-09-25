package dev.malkolm.recipeapp.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The on-disk backup format: a zip file containing this, serialized to JSON, as its
 * `manifest.json` entry, plus a copy of every photo/PDF attachment under `files/<original
 * relative path>` (see [RecipeBackupService]). [BackupAttachment]'s `filePath`/`thumbnailPath`
 * fields are exactly [dev.malkolm.recipeapp.domain.model.Attachment]'s, so a file only needs
 * moving into place, never rewriting, on import.
 */
@Serializable
data class BackupManifest(
    val version: Int = 2,
    val recipes: List<BackupRecipe>,
    /** Since version 2; empty in older backups and in single shared recipes. */
    val mealPlan: List<BackupPlanEntry> = emptyList(),
    /** Since version 2; `null` in older backups and in single shared recipes. */
    val settings: BackupSettings? = null
)

/** A meal-plan entry: [kind] "MEAL" (with [mealType] and [recipeId]) or "GROCERY". [date] is ISO, "2026-09-28". */
@Serializable
data class BackupPlanEntry(
    val id: String,
    val date: String,
    val kind: String,
    val mealType: String? = null,
    val recipeId: String? = null
)

/**
 * The user's settings. Every field is optional so a backup can be read by versions with more or
 * fewer settings. Not included: the chosen phone calendar, which only exists on the old phone.
 * [shoppingListImagePath] is a `backgrounds/` file stored in the backup like recipe photos.
 */
@Serializable
data class BackupSettings(
    val themeMode: String? = null,
    val backgroundBlur: Int? = null,
    val shoppingListImagePath: String? = null,
    val weeksAhead: Int? = null,
    val mealsPerDay: Int? = null,
    /** Meal type name to "HH:mm". */
    val mealTimes: Map<String, String> = emptyMap(),
    val groceryTime: String? = null
)

@Serializable
data class BackupRecipe(
    val id: String,
    val title: String,
    val ingredients: String,
    /** Missing in backups made before the method field existed, hence the default. */
    val method: String = "",
    val servings: Int? = null,
    val cookingTimeMinutes: Int? = null,
    val rating: Int? = null,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val attachments: List<BackupAttachment> = emptyList()
)

@Serializable
sealed interface BackupAttachment {
    val id: String
    val title: String?

    @Serializable
    @SerialName("image")
    data class Image(override val id: String, val filePath: String, override val title: String? = null) :
        BackupAttachment

    @Serializable
    @SerialName("pdf")
    data class Pdf(
        override val id: String,
        val filePath: String,
        override val title: String? = null,
        val thumbnailPath: String? = null
    ) : BackupAttachment

    @Serializable
    @SerialName("link")
    data class Link(
        override val id: String,
        val url: String,
        override val title: String? = null,
        val thumbnailPath: String? = null
    ) : BackupAttachment

    @Serializable
    @SerialName("text")
    data class Text(override val id: String, val text: String, override val title: String? = null) :
        BackupAttachment
}
