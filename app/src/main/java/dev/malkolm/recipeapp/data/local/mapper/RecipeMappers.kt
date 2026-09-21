package dev.malkolm.recipeapp.data.local.mapper

import dev.malkolm.recipeapp.data.local.entity.AttachmentEntity
import dev.malkolm.recipeapp.data.local.entity.AttachmentType
import dev.malkolm.recipeapp.data.local.entity.RecipeEntity
import dev.malkolm.recipeapp.data.local.entity.TagEntity
import dev.malkolm.recipeapp.data.local.relation.RecipeSummaryRow
import dev.malkolm.recipeapp.data.local.relation.RecipeWithDetails
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.Recipe
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.RecipeSummary
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.domain.model.TagNames
import java.time.Instant

// Converts between database rows (entities) and the plain models the rest of the app uses. Keeping
// this in one place means the database layout can change without touching the UI.

/** A new (or updated) recipe row. `createdAt` is only used if the recipe does not exist yet. */
fun RecipeDraft.toEntity(now: Instant): RecipeEntity = RecipeEntity(
    id = id,
    title = title,
    ingredients = ingredients,
    servings = servings,
    cookingTimeMinutes = cookingTimeMinutes,
    rating = rating,
    notes = notes,
    createdAt = now.toEpochMilli(),
    updatedAt = now.toEpochMilli(),
    deletedAt = null
)

fun Attachment.toEntity(recipeId: String, position: Int): AttachmentEntity = when (this) {
    is Attachment.Image ->
        AttachmentEntity(
            id = id,
            recipeId = recipeId,
            type = AttachmentType.IMAGE,
            position = position,
            title = title,
            url = null,
            text = null,
            filePath = filePath,
            thumbnailPath = null
        )

    is Attachment.Pdf ->
        AttachmentEntity(
            id = id,
            recipeId = recipeId,
            type = AttachmentType.PDF,
            position = position,
            title = title,
            url = null,
            text = null,
            filePath = filePath,
            thumbnailPath = thumbnailPath
        )

    is Attachment.Link ->
        AttachmentEntity(
            id = id,
            recipeId = recipeId,
            type = AttachmentType.LINK,
            position = position,
            title = title,
            url = url,
            text = null,
            filePath = null,
            thumbnailPath = thumbnailPath
        )

    is Attachment.Text ->
        AttachmentEntity(
            id = id,
            recipeId = recipeId,
            type = AttachmentType.TEXT,
            position = position,
            title = title,
            url = null,
            text = text,
            filePath = null,
            thumbnailPath = null
        )
}

fun List<Attachment>.toEntities(recipeId: String): List<AttachmentEntity> =
    mapIndexed { position, attachment -> attachment.toEntity(recipeId, position) }

/** A row that lacks the column its type needs means the database is corrupt, so fail loudly. */
fun AttachmentEntity.toDomain(): Attachment = when (type) {
    AttachmentType.IMAGE ->
        Attachment.Image(
            id = id,
            filePath = checkNotNull(filePath) { "IMAGE attachment $id has no filePath" },
            title = title
        )

    AttachmentType.PDF ->
        Attachment.Pdf(
            id = id,
            filePath = checkNotNull(filePath) { "PDF attachment $id has no filePath" },
            title = title,
            thumbnailPath = thumbnailPath
        )

    AttachmentType.LINK ->
        Attachment.Link(
            id = id,
            url = checkNotNull(url) { "LINK attachment $id has no url" },
            title = title,
            thumbnailPath = thumbnailPath
        )

    AttachmentType.TEXT ->
        Attachment.Text(
            id = id,
            text = checkNotNull(text) { "TEXT attachment $id has no text" },
            title = title
        )
}

fun Tag.toEntity(): TagEntity {
    val cleaned = TagNames.clean(name)
    return TagEntity(id = id, name = cleaned, normalizedName = TagNames.key(cleaned))
}

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name)

fun RecipeWithDetails.toDomain(): Recipe = Recipe(
    id = recipe.id,
    title = recipe.title,
    ingredients = recipe.ingredients,
    servings = recipe.servings,
    cookingTimeMinutes = recipe.cookingTimeMinutes,
    rating = recipe.rating,
    notes = recipe.notes,
    // The database does not guarantee the order of related rows, so order them explicitly.
    tags = tags.sortedBy { it.normalizedName }.map { it.toDomain() },
    attachments = attachments.sortedBy { it.position }.map { it.toDomain() },
    createdAt = Instant.ofEpochMilli(recipe.createdAt),
    updatedAt = Instant.ofEpochMilli(recipe.updatedAt)
)

fun RecipeSummaryRow.toDomain(): RecipeSummary = RecipeSummary(
    id = id,
    title = title,
    servings = servings,
    cookingTimeMinutes = cookingTimeMinutes,
    rating = rating,
    thumbnailPath = thumbnailPath,
    updatedAt = Instant.ofEpochMilli(updatedAt)
)
