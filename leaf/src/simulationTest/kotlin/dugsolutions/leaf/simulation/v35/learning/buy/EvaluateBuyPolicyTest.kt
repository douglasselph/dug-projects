package dugsolutions.leaf.simulation.v35.learning.buy

import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluateBuyPolicyTest {
    @Test fun `evaluation accumulator starts empty and keeps four seat buckets`() {
        val a=EvalAccumulator()
        assertEquals(0.0,a.winShare)
        assertEquals(0L,a.plantPurchases)
        assertEquals(4,a.seatWins.size)
        assertEquals(4,a.seatGames.size)
    }
}
