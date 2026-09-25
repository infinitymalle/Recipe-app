package dev.malkolm.recipeapp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountScalingTest {
    @Test
    fun `whole numbers and decimals scale and keep their units`() {
        assertEquals("4 dl", scaleAmount("2 dl", 2.0))
        assertEquals("600 g can", scaleAmount("400 g can", 1.5))
        assertEquals("3", scaleAmount("1,5", 2.0))
        assertEquals("1.2 kg", scaleAmount("0.8 kg", 1.5))
    }

    @Test
    fun `fractions and mixed numbers come out as kitchen fractions`() {
        assertEquals("1 tsp", scaleAmount("1/2 tsp", 2.0))
        assertEquals("3 tbsp", scaleAmount("1 1/2 tbsp", 2.0))
        assertEquals("1/4 tsp", scaleAmount("1/2 tsp", 0.5))
        assertEquals("4 1/2", scaleAmount("3", 1.5))
        assertEquals("1 1/2 dl", scaleAmount("½ dl", 3.0))
        assertEquals("2/3 dl", scaleAmount("1 ⅓ dl", 0.5))
    }

    @Test
    fun `every number in a range or phrase is scaled`() {
        assertEquals("6-8 tbsp", scaleAmount("3-4 tbsp", 2.0))
        assertEquals("about 26 dl", scaleAmount("about 13 dl", 2.0))
    }

    @Test
    fun `text without numbers and a factor of 1 are left alone`() {
        assertEquals("a pinch", scaleAmount("a pinch", 3.0))
        assertEquals("1 1/2 tbsp", scaleAmount("1 1/2 tbsp", 1.0))
    }

    @Test
    fun `formatting rounds sensibly`() {
        assertEquals("12", formatAmount(12.4))
        assertEquals("2", formatAmount(1.97))
        assertEquals("1/3", formatAmount(1.0 / 3))
        assertEquals("0.2", formatAmount(0.2))
    }
}
