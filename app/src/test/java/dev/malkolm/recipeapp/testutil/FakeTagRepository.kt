package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.domain.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [TagRepository] for ViewModel tests. */
class FakeTagRepository(tags: List<Tag> = emptyList()) : TagRepository {
    private val tagsInUse = MutableStateFlow(tags)

    override fun observeTagsInUse() = tagsInUse
}
