package dev.malkolm.recipeapp.data

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UuidIdGeneratorTest {
    @Test
    fun `ids are valid UUIDs and do not repeat`() {
        val generator = UuidIdGenerator()

        val ids = List(1_000) { generator.newId() }

        assertEquals(1_000, ids.toSet().size)
        ids.forEach { UUID.fromString(it) } // throws if the format is wrong
    }
}
