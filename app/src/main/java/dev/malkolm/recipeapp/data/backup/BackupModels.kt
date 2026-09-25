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
data class BackupManifest(val version: Int = 1, val recipes: List<BackupRecipe>)

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
