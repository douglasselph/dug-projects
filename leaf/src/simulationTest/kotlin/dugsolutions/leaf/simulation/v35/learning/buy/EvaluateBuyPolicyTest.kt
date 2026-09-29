package dugsolutions.leaf.simulation.v35.learning.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EvaluateBuyPolicyTest {
    @Test fun `evaluation accumulator starts empty and keeps four seat buckets`() {
        val a=EvalAccumulator()
        assertEquals(0.0,a.winShare)
        assertEquals(0L,a.plantPurchases)
        assertEquals(4,a.seatWins.size)
        assertEquals(4,a.seatGames.size)
    }
    @Test fun `default evaluation keeps FirstGameDefault`() {
        val o = EvalOptions.parse(emptyList())
        assertNull(o.grovePattern)
        assertEquals(181000L, o.groveSeed)
    }

    @Test fun `grove option preserves partial Grove pattern and dedicated seed`() {
        val o = EvalOptions.parse(listOf("--grove", "000100000", "--grove-seed", "281000"))
        assertEquals("000100000", o.grovePattern)
        assertEquals(281000L, o.groveSeed)
    }

    @Test fun `random Grove option is all zero pattern`() {
        val o = EvalOptions.parse(listOf("--random-grove"))
        assertEquals("000000000", o.grovePattern)
    }
}
