package dev.malkolm.recipeapp.testutil

import dev.malkolm.recipeapp.domain.IdGenerator

/** Deterministic [IdGenerator] for tests: `"id-0"`, `"id-1"`, ... */
class SequentialIdGenerator(private val prefix: String = "id") : IdGenerator {
    private var counter = 0

    override fun newId(): String = "$prefix-${counter++}"
}
