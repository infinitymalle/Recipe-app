package dev.malkolm.recipeapp.data

import dev.malkolm.recipeapp.domain.IdGenerator
import java.util.UUID
import javax.inject.Inject

class UuidIdGenerator
@Inject
constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
