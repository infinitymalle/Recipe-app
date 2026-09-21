package dev.malkolm.recipeapp.domain.model

/**
 * A label such as "dessert" or "vegetarian" that can be attached to many recipes.
 *
 * Build new tags with [Tag.of] so the name is cleaned; the repository merges tags whose
 * [key] matches, so typing "dessert" twice never creates two tags.
 */
data class Tag(val id: String, val name: String) {
    init {
        require(name.isNotBlank()) { "Tag name must not be blank" }
    }

    /** Case-insensitive identity of the tag. */
    val key: String get() = TagNames.key(TagNames.clean(name))

    companion object {
        fun of(id: String, rawName: String): Tag = Tag(id = id, name = TagNames.clean(rawName))
    }
}
