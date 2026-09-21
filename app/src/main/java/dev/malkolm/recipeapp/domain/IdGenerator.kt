package dev.malkolm.recipeapp.domain

/**
 * Creates ids for new recipes, tags and attachments.
 *
 * Ids are random UUID strings created on the device, not database counters. Two phones can then
 * create records offline and later be merged without id collisions, which is what makes adding
 * cloud sync possible without rewriting the data model.
 */
fun interface IdGenerator {
    fun newId(): String
}
