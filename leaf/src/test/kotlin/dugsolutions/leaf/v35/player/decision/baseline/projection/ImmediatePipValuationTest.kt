package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.operation.RollRewardPolicy
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImmediatePipValuationTest {
    private val available = RollRewardOpportunities(critterAvailable = true, wispAvailable = true)
    private val none = RollRewardOpportunities(critterAvailable = false, wispAvailable = false)

    @Test fun `D10 expected face and roll rewards remain separate`() {
        val value = ImmediatePipValuation.roll(10, available)
        assertEquals(5.5, value.spendingPips)
        assertEquals(0.2, value.critterOptionValue, 1e-12)
        assertEquals(0.3, value.wispOptionValue, 1e-12)
        assertEquals(6.0, value.pipEquivalentTotal, 1e-12)
    }

    @Test fun `D4 and D20 reward probabilities use their own face counts`() {
        val d4 = ImmediatePipValuation.roll(4, available)
        assertEquals(2.5, d4.spendingPips)
        assertEquals(3.75, d4.pipEquivalentTotal, 1e-12)
        val d20 = ImmediatePipValuation.roll(20, available)
        assertEquals(10.5, d20.spendingPips)
        assertEquals(10.75, d20.pipEquivalentTotal, 1e-12)
    }

    @Test fun `reward availability is independent and ignores empty resources`() {
        assertEquals(5.5, ImmediatePipValuation.roll(10, none).pipEquivalentTotal)
        val critterOnly = RollRewardOpportunities(true, false)
        val wispOnly = RollRewardOpportunities(false, true)
        assertEquals(5.7, ImmediatePipValuation.roll(10, critterOnly).pipEquivalentTotal, 1e-12)
        assertEquals(5.8, ImmediatePipValuation.roll(10, wispOnly).pipEquivalentTotal, 1e-12)
    }

    @Test fun `ignored and uncommitted deferred rolls create no speculative rewards`() {
        for (policy in listOf(RollRewardPolicy.IGNORE, RollRewardPolicy.DEFER)) {
            val roll = ImmediatePipValuation.roll(10, available, policy)
            assertEquals(5.5, roll.spendingPips)
            assertEquals(0.0, roll.critterOptionValue)
            assertEquals(0.0, roll.wispOptionValue)
            val reroll = ImmediatePipValuation.reroll(10, 9, available, policy)
            assertEquals(-3.5, reroll.spendingPips)
            assertEquals(-3.5, reroll.pipEquivalentTotal)
        }
    }

    @Test fun `multiple draws obey lowest supply then discard recycle`() {
        val drawn = ImmediatePipValuation.draws(
            supply = listOf(20, 4), discard = listOf(6), count = 5,
            opportunities = available
        )
        // D4, D20, D6; no imaginary fourth or fifth die.
        assertEquals(2.5 + 10.5 + 3.5, drawn.spendingPips)
        assertEquals(2.0 / 4 + 2.0 / 20 + 2.0 / 6, drawn.critterOptionValue, 1e-12)
        assertEquals(3.0 / 4 + 3.0 / 20 + 3.0 / 6, drawn.wispOptionValue, 1e-12)
        assertEquals(ImmediatePipValue(0.0),
            ImmediatePipValuation.draws(emptyList(), emptyList(), 2, available))
    }

    @Test fun `reroll produces signed spending change plus only fresh eligible rewards`() {
        val value = ImmediatePipValuation.reroll(10, 9, available)
        assertEquals(-3.5, value.spendingPips)
        assertEquals(-3.0, value.pipEquivalentTotal, 1e-12)
    }

    @Test fun `upgrade and use now is a replacement roll not future die value`() {
        val value = ImmediatePipValuation.upgradeUseNow(12, 8, 20, available)
        assertEquals(2.5, value.spendingPips)
        assertEquals(2.75, value.pipEquivalentTotal, 1e-12)
        assertEquals(-0.5, ImmediatePipValuation.upgradeUseNow(4, 4, 6, none).spendingPips)
        assertThrows(IllegalArgumentException::class.java) {
            ImmediatePipValuation.upgradeUseNow(12, 8, 10, available)
        }
    }

    @Test fun `Raise and other face changes award no new roll rewards`() {
        val raise = ImmediatePipValuation.raise(20, 18, 4)
        assertEquals(2.0, raise.spendingPips)
        assertEquals(2.0, raise.pipEquivalentTotal)
        assertEquals(0.0, ImmediatePipValuation.raise(8, 8, 4).pipEquivalentTotal)
        assertEquals(2.0, ImmediatePipValuation.faceEffect(GameEffect.RAISE_DIE_PLUS_4, 20, 18)?.spendingPips)
        assertEquals(-3.0, ImmediatePipValuation.faceEffect(GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE, 6, 5)?.spendingPips)
        assertNull(ImmediatePipValuation.faceEffect(GameEffect.DRAW_TWO_DICE, 20, 1))
    }

    @Test fun `visible default context has no available reward resources`() {
        val empty = RollRewardOpportunities.fromVisibleContext(DecisionContext.EMPTY)
        assertFalse(empty.critterAvailable)
        assertFalse(empty.wispAvailable)
        val withResources = DecisionContext.EMPTY.copy(
            grove = DecisionContext.EMPTY.grove.copy(bees = 1, wispDeckRemaining = 3)
        )
        val found = RollRewardOpportunities.fromVisibleContext(withResources)
        assertTrue(found.critterAvailable)
        assertTrue(found.wispAvailable)
    }

    @Test fun `value rejects invalid inputs instead of inventing benefit`() {
        assertThrows(IllegalArgumentException::class.java) { ImmediatePipValuation.roll(0, none) }
        assertThrows(IllegalArgumentException::class.java) { ImmediatePipValuation.reroll(8, 0, none) }
        assertThrows(IllegalArgumentException::class.java) {
            ImmediatePipValuation.draws(emptyList(), emptyList(), -1, none)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RollRewardOpportunities(true, true, Double.NaN, 3.0)
        }
    }
}
