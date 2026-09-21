package dev.malkolm.recipeapp.data.repository

import dev.malkolm.recipeapp.data.local.dao.TagDao
import dev.malkolm.recipeapp.data.local.mapper.toDomain
import dev.malkolm.recipeapp.domain.model.Tag
import dev.malkolm.recipeapp.domain.repository.TagRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTagRepository
@Inject
constructor(private val dao: TagDao) : TagRepository {
    override fun observeTagsInUse(): Flow<List<Tag>> = dao.observeTagsInUse().map { rows ->
        rows.map { it.toDomain() }
    }
}
