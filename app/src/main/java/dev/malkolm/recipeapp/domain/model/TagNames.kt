package dev.malkolm.recipeapp.domain.model

import java.text.Normalizer

/**
 * Rules for turning what the user typed into a tag name.
 *
 * Tags are unique ignoring case ("Dessert" and "dessert" are the same tag). SQLite's built-in
 * case-insensitive comparison only understands A-Z, so it would treat "Äpple" and "äpple" as
 * different tags. We do the folding here in Kotlin instead, which handles å, ä, ö and friends.
 */
object TagNames {
    private val whitespace = Regex("\\s+")

    /** The name as shown to the user: trimmed, single spaces, composed Unicode form. */
    fun clean(raw: String): String = Normalizer.normalize(raw, Normalizer.Form.NFC).trim().replace(whitespace, " ")

    /** The key used to decide whether two names are the same tag. Always applied to a cleaned name. */
    fun key(cleaned: String): String = cleaned.lowercase()
}
