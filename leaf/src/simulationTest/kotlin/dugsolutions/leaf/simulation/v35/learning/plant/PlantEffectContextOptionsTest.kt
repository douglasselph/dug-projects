package dugsolutions.leaf.simulation.v35.learning.plant

import kotlin.test.Test
import kotlin.test.assertEquals

class PlantEffectContextOptionsTest {
    @Test
    fun `Plant Effect trainer accepts learned Buy companion`() {
        val o = PlantEffectTrainOptions.parse(listOf(
            "--buy-policy", "learned",
            "--buy-weights", "output/buy.weights"
        ))
        assertEquals("learned", o.buyPolicy)
        assertEquals("output/buy.weights", o.buyWeights.toString())
    }
}
