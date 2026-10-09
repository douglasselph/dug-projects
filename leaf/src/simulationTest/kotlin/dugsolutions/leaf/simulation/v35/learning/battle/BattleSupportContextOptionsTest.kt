package dugsolutions.leaf.simulation.v35.learning.battle

import kotlin.test.Test
import kotlin.test.assertEquals

class BattleSupportContextOptionsTest {
    @Test
    fun `Battle Support trainer accepts learned Buy companion`() {
        val o = BattleSupportTrainOptions.parse(listOf(
            "--buy-policy", "learned",
            "--buy-weights", "output/buy.weights",
            "--random-grove"
        ))
        assertEquals("learned", o.buyPolicy)
        assertEquals("output/buy.weights", o.buyWeights.toString())
        assertEquals("000000000", o.grovePattern)
    }
}
