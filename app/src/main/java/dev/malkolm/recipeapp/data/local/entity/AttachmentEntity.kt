package dev.malkolm.recipeapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stored in the database as its name. Never rename or reorder existing values: doing so would
 * make rows already on her phone unreadable. Adding a new value is safe.
 */
enum class AttachmentType { IMAGE, LINK, TEXT, PDF }

/**
 * One row in `attachments`. Which of [url], [text] and [filePath] is filled depends on [type]
 * (see the mapper). Only paths are stored here, never file contents.
 *
 * The foreign key with `CASCADE` means that if a recipe row is ever removed for real, its
 * attachments go with it and no orphan rows remain.
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val type: AttachmentType,
    /** Position in the recipe's attachment list, starting at 0. */
    val position: Int,
    val title: String?,
    val url: String?,
    val text: String?,
    val filePath: String?,
    val thumbnailPath: String?
)
