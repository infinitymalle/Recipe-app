package dev.malkolm.recipeapp.domain.model

import kotlin.math.roundToInt

// A recipe's method is stored as one string: its steps as paragraphs separated by a blank line.
// That keeps the database column plain, readable text (like ingredients) with no schema for steps.

private val blankLine = Regex("\n\\s*\n")

/** Splits a method into steps on blank lines; the whole text is one step if there are none. */
fun stepsFrom(method: String): List<String> = method
    .split(blankLine)
    .map { it.trim() }
    .filter { it.isNotEmpty() }

/** The inverse of [stepsFrom]: steps joined with a blank line between them. */
fun List<String>.toMethodText(): String = map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")

/**
 * A cooking time mentioned in a step, e.g. "Bake for 8 to 10 minutes". [minutes] is the lower end
 * of a range: it is safer to check early and add a minute than to find the buns burnt.
 */
data class StepDuration(val minutes: Int, val text: String)

private const val NUMBER = """\d+(?:[.,]\d+)?"""

// English and Swedish: "10 minutes", "8 to 10 min", "1 1/2 hours", "2-3 h", "20 minuter", "1 timme".
private val durationPattern =
    Regex(
        """($NUMBER)(\s+1/2|\s*½)?(?:\s*(?:-|–|to|till|or|eller)\s*($NUMBER))?\s*""" +
            """(hours?|hrs?|timmar|timme|tim|h|minutes?|minuter|minut|mins?)\b""",
        RegexOption.IGNORE_CASE
    )

/** Every cooking time mentioned in [step], in order, for offering one-tap timers. Seconds are ignored. */
fun durationsIn(step: String): List<StepDuration> = durationPattern
    .findAll(step)
    .mapNotNull { match ->
        val (number, half, _, unit) = match.destructured
        var amount = number.replace(',', '.').toDouble()
        if (half.isNotBlank()) amount += 0.5
        val isHours = unit.lowercase().let { it.startsWith("h") || it.startsWith("tim") }
        val minutes = (if (isHours) amount * 60 else amount).roundToInt()
        if (minutes > 0) StepDuration(minutes = minutes, text = match.value.trim()) else null
    }
    .distinctBy { it.minutes }
    .toList()
