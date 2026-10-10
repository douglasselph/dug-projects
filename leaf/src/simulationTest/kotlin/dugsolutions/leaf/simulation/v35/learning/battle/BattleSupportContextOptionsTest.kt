package dugsolutions.leaf.simulation.v35.learning.battle

import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSupportContextOptionsTest {
    @Test
    fun `Battle Support trainer accepts learned Buy companion`() {
        val o = BattleSupportTrainOptions.parse(listOf(
            "--buy-policy", "learned",
            "--buy-weights", "output/buy.weights",
            "--cultivation-main-policy", "learned",
            "--cultivation-main-weights", "output/cult.weights",
            "--plant-effect-policy", "learned",
            "--plant-effect-weights", "output/plant.weights",
            "--random-grove"
        ))
        assertEquals("learned", o.buyPolicy)
        assertEquals("output/buy.weights", o.buyWeights.toString())
        assertEquals("learned", o.cultivationMainPolicy)
        assertEquals("learned", o.plantEffectPolicy)
        assertEquals("000000000", o.grovePattern)
    }
}
