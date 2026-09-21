package dev.malkolm.recipeapp.data.local.mapper

import dev.malkolm.recipeapp.data.local.entity.AttachmentEntity
import dev.malkolm.recipeapp.data.local.entity.AttachmentType
import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecipeMappersTest {
    private val everyKind =
        listOf(
            Attachment.Image(id = "a1", filePath = "recipes/r1/photo.jpg", title = "Result"),
            Attachment.Pdf(
                id = "a2",
                filePath = "recipes/r1/card.pdf",
                title = "Card",
                thumbnailPath = "thumbs/card.png"
            ),
            Attachment.Link(id = "a3", url = "https://example.com/v", title = "Video", thumbnailPath = "thumbs/v.jpg"),
            Attachment.Text(id = "a4", text = "Whisk everything", title = "Method")
        )

    @Test
    fun `every attachment kind survives a round trip through the database row`() {
        val rows = everyKind.toEntities(recipeId = "r1")

        assertEquals(everyKind, rows.map { it.toDomain() })
    }

    @Test
    fun `positions follow the list order`() {
        val rows = everyKind.toEntities(recipeId = "r1")

        assertEquals(listOf(0, 1, 2, 3), rows.map { it.position })
        assertEquals(listOf("r1"), rows.map { it.recipeId }.distinct())
    }

    @Test
    fun `each kind fills only its own columns`() {
        val (image, pdf, link, text) = everyKind.toEntities(recipeId = "r1")

        assertEquals(AttachmentType.IMAGE, image.type)
        assertEquals(null, image.url)
        assertEquals(AttachmentType.LINK, link.type)
        assertEquals(null, link.filePath)
        assertEquals(AttachmentType.TEXT, text.type)
        assertEquals(null, text.filePath)
        assertEquals(AttachmentType.PDF, pdf.type)
        assertEquals("thumbs/card.png", pdf.thumbnailPath)
    }

    @Test
    fun `a row missing the column its type needs is reported as corrupt`() {
        val corrupt =
            AttachmentEntity(
                id = "a1",
                recipeId = "r1",
                type = AttachmentType.LINK,
                position = 0,
                title = null,
                url = null,
                text = null,
                filePath = null,
                thumbnailPath = null
            )

        assertFailsWith<IllegalStateException> { corrupt.toDomain() }
    }

    @Test
    fun `tag rows store the cleaned name and its lower-case key`() {
        val row = Tag(id = "t1", name = "  Äpple   Paj ").toEntity()

        assertEquals("Äpple Paj", row.name)
        assertEquals("äpple paj", row.normalizedName)
    }
}
