package dugsolutions.leaf.simulation.v35.learning.plant

import kotlin.test.Test
import kotlin.test.assertEquals

class PlantEffectContextOptionsTest {
    @Test
    fun `Plant Effect trainer accepts learned Buy companion`() {
        val o = PlantEffectTrainOptions.parse(listOf(
            "--buy-policy", "learned",
            "--buy-weights", "output/buy.weights",
            "--cultivation-main-policy", "learned",
            "--cultivation-main-weights", "output/cult.weights",
            "--battle-support-policy", "learned",
            "--battle-support-weights", "output/battle.weights"
        ))
        assertEquals("learned", o.buyPolicy)
        assertEquals("output/buy.weights", o.buyWeights.toString())
        assertEquals("learned", o.cultivationMainPolicy)
        assertEquals("learned", o.battleSupportPolicy)
    }
}
