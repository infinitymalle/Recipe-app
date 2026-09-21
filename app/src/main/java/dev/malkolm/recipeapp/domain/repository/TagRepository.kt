package dev.malkolm.recipeapp.domain.repository

import dev.malkolm.recipeapp.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    /** Tags that at least one (non-deleted) recipe uses, in alphabetical order. */
    fun observeTagsInUse(): Flow<List<Tag>>
}
