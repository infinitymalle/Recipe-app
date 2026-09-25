package dev.malkolm.recipeapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeStepsTest {
    @Test
    fun `steps are split on blank lines`() {
        assertEquals(emptyList(), stepsFrom(""))
        assertEquals(emptyList(), stepsFrom("   \n\n "))
        assertEquals(listOf("Mix everything."), stepsFrom("Mix everything."))
        assertEquals(
            listOf("Mix flour and water.", "Knead for 10 minutes.", "Let it rest."),
            stepsFrom("Mix flour and water.\n\nKnead for 10 minutes.\n  \nLet it rest.")
        )
    }

    @Test
    fun `steps written back as method text split into the same steps`() {
        val steps = listOf("Mix.", "Line one\nline two of the same step.", "Bake.")

        assertEquals("Mix.\n\nLine one\nline two of the same step.\n\nBake.", steps.toMethodText())
        assertEquals(steps, stepsFrom(steps.toMethodText()))
        assertEquals("A.\n\nB.", listOf(" A. ", "", "B.").toMethodText())
    }

    private fun minutesIn(step: String) = durationsIn(step).map { it.minutes }

    @Test
    fun `finds minutes and hours in English`() {
        assertEquals(listOf(10), minutesIn("Knead for 10 minutes until smooth."))
        assertEquals(listOf(5), minutesIn("Rest 5 min."))
        assertEquals(listOf(90), minutesIn("Simmer for 1 1/2 hours."))
        assertEquals(listOf(120), minutesIn("Cook for 2 hrs."))
        assertEquals(listOf(45), minutesIn("Roast for 0.75 h"))
    }

    @Test
    fun `a range starts the timer at its lower end`() {
        assertEquals(listOf(8), minutesIn("Bake for 8 to 10 minutes, until golden."))
        assertEquals(listOf(2), minutesIn("Cook 2-3 min per side."))
        assertEquals(listOf(30), minutesIn("Rise for 30–45 minutes."))
    }

    @Test
    fun `finds times in Swedish`() {
        assertEquals(listOf(20), minutesIn("Koka i 20 minuter."))
        assertEquals(listOf(60), minutesIn("Låt jäsa 1 timme."))
        assertEquals(listOf(8), minutesIn("Grädda 8 till 10 min."))
    }

    @Test
    fun `several times in one step each get a timer, once each`() {
        assertEquals(listOf(3, 10), minutesIn("Fry for 3 minutes, then simmer for 10 minutes. Stir after 3 minutes."))
    }

    @Test
    fun `numbers that are not times are ignored`() {
        assertEquals(emptyList(), minutesIn("Heat the oven to 225 °C and roll out to 30 x 40 cm."))
        assertEquals(emptyList(), minutesIn("Cook for 30 seconds."))
        assertEquals(emptyList(), minutesIn("Add 2 dl milk and 1 tbsp sugar."))
    }
}
