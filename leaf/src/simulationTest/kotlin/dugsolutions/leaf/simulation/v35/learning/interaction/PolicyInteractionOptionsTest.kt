package dugsolutions.leaf.simulation.v35.learning.interaction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PolicyInteractionOptionsTest {
    @Test
    fun `parses independent learned policies and paths`() {
        val o = PolicyInteractionOptions.parse(listOf(
            "--samples", "17",
            "--players", "3",
            "--rounds", "3/2/2",
            "--buy-policy", "learned", "--buy-weights", "b.weights",
            "--cultivation-main-policy", "learned", "--cultivation-main-weights", "c.weights",
            "--cultivation-support-policy", "learned", "--cultivation-support-weights", "cs.weights",
            "--battle-support-policy", "learned", "--battle-support-weights", "s.weights"
        ))
        assertEquals(17, o.games)
        assertEquals(3, o.players)
        assertEquals("learned", o.buyPolicy)
        assertEquals("b.weights", o.buyWeights.toString())
        assertEquals("learned", o.cultivationMainPolicy)
        assertEquals("c.weights", o.cultivationMainWeights.toString())
        assertEquals("learned", o.cultivationSupportPolicy)
        assertEquals("cs.weights", o.cultivationSupportWeights.toString())
        assertEquals("learned", o.battleSupportPolicy)
        assertEquals("s.weights", o.battleSupportWeights.toString())
    }

    @Test
    fun `rejects invalid policy family`() {
        assertFailsWith<IllegalArgumentException> {
            PolicyInteractionOptions.parse(listOf("--battle-support-policy", "magic"))
        }
        assertFailsWith<IllegalArgumentException> {
            PolicyInteractionOptions.parse(listOf("--cultivation-support-policy", "magic"))
        }
    }
}
