package dev.malkolm.recipeapp.domain.model

/**
 * Something saved together with a recipe: a photo, a link, pasted text or a PDF.
 *
 * Each kind is its own type, so a [Link] always has a URL and an [Image] always has a file.
 * The database stores these in one table with nullable columns; the mapping code is the only
 * place that has to deal with that.
 *
 * All `filePath` / `thumbnailPath` values are **relative** to the app's private files directory
 * (for example `recipes/<id>/photo1.jpg`), never absolute. That keeps them valid after a backup
 * restore, and image bytes never go in the database.
 *
 * The order of attachments inside a recipe is the order of the list.
 */
sealed interface Attachment {
    val id: String

    data class Image(override val id: String, val filePath: String, val title: String? = null) : Attachment

    data class Pdf(
        override val id: String,
        val filePath: String,
        val title: String? = null,
        val thumbnailPath: String? = null
    ) : Attachment

    /** A web or video link. For videos only the link, title and thumbnail are kept, never the video. */
    data class Link(
        override val id: String,
        val url: String,
        val title: String? = null,
        val thumbnailPath: String? = null
    ) : Attachment

    data class Text(override val id: String, val text: String, val title: String? = null) : Attachment
}
