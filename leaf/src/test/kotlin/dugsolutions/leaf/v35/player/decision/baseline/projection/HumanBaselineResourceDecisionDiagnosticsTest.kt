package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.ButterflyView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.tokens.Butterfly
import org.junit.jupiter.api.Test
import kotlin.test.*

/** Constructed contrasts make unintended fixed bonuses and phantom resources visible. */
class HumanBaselineResourceDecisionDiagnosticsTest {
    private val rival = PlayerId(2)
    private fun state(groveBees: Int, opponentBees: Int): DecisionContext = DecisionContext.EMPTY.copy(
        grove = DecisionContext.EMPTY.grove.copy(bees = groveBees),
        opponents = listOf(OpponentView(
            DecisionContext.EMPTY.self.board.copy(id = rival, bees = opponentBees), 0
        ))
    )

    @Test fun `Bee acquisition does not score non-existent Grove or opponent supplies`() {
        val empty = state(0, 0)
        assertNull(PlantResourceProjection.beeSource(empty, null))
        assertNull(PlantResourceProjection.beeSource(empty, rival))
        assertNotNull(PlantResourceProjection.beeSource(state(1, 0), null))
        assertNotNull(PlantResourceProjection.beeSource(state(0, 1), rival))
    }

    @Test fun `stealing Bee includes opponent denial when Grove alternative is legal`() {
        val context = state(1, 1)
        val grove = assertNotNull(PlantResourceProjection.beeSource(context, null))
        val steal = assertNotNull(PlantResourceProjection.beeSource(context, rival))
        assertTrue(steal > grove, "Stealing a Bee must account for denying the rival")
        assertEquals(steal, PlantResourceProjection.beeSource(context, rival))
    }

    @Test fun `Butterfly denial distinguishes ready from spent targets`() {
        fun stateWithButterfly(ready: Boolean) = DecisionContext.EMPTY.copy(
            opponents = listOf(OpponentView(
                DecisionContext.EMPTY.self.board.copy(id = rival,
                    butterflies = listOf(ButterflyView(Butterfly.YELLOW, isFaceUp = ready))), 0
            ))
        )
        val ready = assertNotNull(PlantResourceProjection.butterflySource(stateWithButterfly(true), rival, Butterfly.YELLOW))
        val spent = assertNotNull(PlantResourceProjection.butterflySource(stateWithButterfly(false), rival, Butterfly.YELLOW))
        assertTrue(ready > spent, "Stealing a ready Butterfly denies an immediate reroll")
    }

    @Test fun `Butterfly refresh credits exactly the count currently spent`() {
        fun withButterflies(vararg ready: Boolean) = DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                butterflies = ready.mapIndexed { i, isReady ->
                    ButterflyView(listOf(Butterfly.GREEN, Butterfly.YELLOW, Butterfly.RED)[i], isFaceUp = isReady)
                }
            ))
        )
        val zero = PlantResourceProjection.butterflyRefresh(withButterflies(true, true))
        val one = PlantResourceProjection.butterflyRefresh(withButterflies(false, true))
        val two = PlantResourceProjection.butterflyRefresh(withButterflies(false, false))
        assertEquals(0, zero)
        assertTrue(two > one && one > zero)
    }

    @Test fun `Bee sequence value follows actual legal source and available count`() {
        val noSource = state(0, 0)
        val source = state(1, 0)
        val effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
        assertTrue(PlantResourceProjection.beeSequence(source, effect) > PlantResourceProjection.beeSequence(noSource, effect))
    }
}
