package dev.malkolm.recipeapp.domain.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// Scales the numbers in an ingredient amount for a different number of servings, e.g. "1 1/2 dl"
// x2 = "3 dl". Only for display: the saved recipe is never changed, since scaled recipes rarely
// taste exactly the same (spices, leavening and baking times do not scale linearly).

private val unicodeFractions = mapOf('½' to 0.5, '¼' to 0.25, '¾' to 0.75, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3)

// Mixed number ("1 1/2"), fraction ("1/2"), decimal ("1.5" or "1,5"), or a unicode fraction ("½").
// The space before a unicode fraction is only part of the match when the fraction follows, so
// "2 dl" keeps its space.
private val numberPattern = Regex("""(\d+)\s+(\d+)/(\d+)|(\d+)/(\d+)|(\d+(?:[.,]\d+)?)(?:\s*([½¼¾⅓⅔]))?|([½¼¾⅓⅔])""")

/** [amount] with every number in it multiplied by [factor]; unchanged when [factor] is 1. */
fun scaleAmount(amount: String, factor: Double): String {
    if (factor == 1.0) return amount
    return numberPattern.replace(amount) { match ->
        val g = match.groupValues
        val value =
            when {
                g[1].isNotEmpty() -> g[1].toDouble() + g[2].toDouble() / g[3].toDouble()

                g[4].isNotEmpty() -> g[4].toDouble() / g[5].toDouble()

                g[6].isNotEmpty() -> g[6].replace(',', '.').toDouble() +
                    (g[7].firstOrNull()?.let(unicodeFractions::get) ?: 0.0)

                else -> unicodeFractions.getValue(g[8].first())
            }
        formatAmount(value * factor)
    }
}

private val niceFractions = listOf(
    1.0 / 4 to "1/4",
    1.0 / 3 to "1/3",
    1.0 / 2 to "1/2",
    2.0 / 3 to "2/3",
    3.0 / 4 to "3/4"
)

/** Kitchen-friendly: "3", "1 1/2", "2/3", or one decimal ("1.2") when no simple fraction fits. */
fun formatAmount(value: Double): String {
    if (value >= 10) return value.roundToInt().toString()
    val whole = floor(value)
    val rest = value - whole
    if (rest < 0.05) return whole.toInt().toString()
    if (rest > 0.95) return (whole.toInt() + 1).toString()
    niceFractions.firstOrNull { (fraction, _) -> abs(rest - fraction) < 0.04 }?.let { (_, text) ->
        return if (whole == 0.0) text else "${whole.toInt()} $text"
    }
    return ((value * 10).roundToInt() / 10.0).toString()
}
