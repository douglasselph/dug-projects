package dugsolutions.leaf.simulation.v35.learning.buy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderResourceSnapshot
import dugsolutions.leaf.v35.chronicle.domain.BuyOrderDieSnapshot
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides

class EvaluateBuyPolicyTest {
    @Test fun `evaluation accumulator starts empty and keeps four seat buckets`() {
        val a=EvalAccumulator()
        assertEquals(0.0,a.winShare)
        assertEquals(0L,a.plantPurchases)
        assertEquals(4,a.seatWins.size)
        assertEquals(4,a.seatGames.size)
    }
    @Test fun `buy shape groups purchases within each Buy phase and records zero purchase phases`() {
        val p = PlayerId(1)
        val entries = listOf(
            GameEntry.BuyOrder(1, listOf(p), listOf(BuyOrderResourceSnapshot(p, listOf(BuyOrderDieSnapshot(DieSides.D20, 17)), emptyList()))),
            GameEntry.Purchase(2, p, PurchaseKind.PLANT, "Vine_07_01", 7, 7),
            GameEntry.Purchase(3, p, PurchaseKind.PLANT, "Vine_07_01", 7, 10),
            GameEntry.BuyOrder(4, listOf(p), listOf(BuyOrderResourceSnapshot(p, emptyList(), emptyList())))
        )
        val shape = BuyShapeAccumulator()
        shape.addGame(entries, p)
        assertEquals(2L, shape.phases)
        assertEquals(2L, shape.purchases)
        assertEquals(2, shape.maxPurchases)
        assertEquals(1L, shape.purchaseCount[0])
        assertEquals(1L, shape.purchaseCount[2])
        assertEquals(1L, shape.kindSequences["Plant -> Plant"])
        assertEquals(1L, shape.itemSequences["P7 -> P7"])
        assertEquals(1L, shape.plantCostSequences["7 -> 7"])
        assertEquals(17L, shape.startingPower)
        assertEquals(17L, shape.spentPower)
        assertEquals(3L, shape.overpayment)
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
