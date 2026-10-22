package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.ButterflyView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.tokens.Butterfly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlantResourceProjectionTest {
    @Test
    fun `refresh counts only spent butterflies`() {
        val before = DecisionContext.EMPTY
        val context = before.copy(self = before.self.copy(board = before.self.board.copy(
            butterflies = listOf(ButterflyView(Butterfly.RED, false), ButterflyView(Butterfly.GREEN, true))
        )))
        assertEquals(10, PlantResourceProjection.butterflyRefresh(context))
        assertEquals(0, PlantResourceProjection.butterflyRefresh(before))
    }

    @Test
    fun `stealing ready butterfly values denial more than spent butterfly`() {
        val before = DecisionContext.EMPTY
        val opponent = OpponentView(before.self.board.copy(
            id = PlayerId(1),
            butterflies = listOf(ButterflyView(Butterfly.RED, true), ButterflyView(Butterfly.GREEN, false))
        ), wispCount = 0)
        val context = before.copy(opponents = listOf(opponent))
        val ready = PlantResourceProjection.butterflySource(context, opponent.id, Butterfly.RED)!!
        val spent = PlantResourceProjection.butterflySource(context, opponent.id, Butterfly.GREEN)!!
        assertTrue(ready > spent)
        assertNull(PlantResourceProjection.butterflySource(context, null, Butterfly.RED))
        assertTrue(PlantResourceProjection.butterflySequence(
            context, GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES
        ) >= ready)
        assertEquals(emptyList(), before.opponents)
    }

    @Test
    fun `bee gain depends on legal supply and round value`() {
        val before = DecisionContext.EMPTY
        val context = before.copy(
            grove = before.grove.copy(bees = 1),
            self = before.self.copy(board = before.self.board.copy(bees = 2, beeValue = 2))
        )
        assertNull(PlantResourceProjection.beeSource(before, null))
        assertTrue(PlantResourceProjection.beeSource(context, null)!! > 0)
        val normal = PlantResourceProjection.beeSequence(
            context, GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
        )
        assertTrue(normal > 0)
        assertEquals(0, before.self.board.bees)
    }
}
