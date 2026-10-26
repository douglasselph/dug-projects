package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ActionValueTest {
    @Test fun `computed components and effective effect are retained for cultivation`() {
        val value = ActionValue(
            phase = CardPhase.CULTIVATION,
            effectiveEffect = GameEffect.RAISE_DIE_PLUS_4,
            candidate = "Root Four More",
            target = "D6 showing 2",
            legal = true,
            calculatedBenefit = 4.0,
            contextualBonuses = 1.5,
            contextualCosts = 0.5,
            uncertainty = -0.25,
            variability = 0.75
        )
        assertEquals(5.5, value.finalPreference)
        assertEquals("D6 showing 2", value.target)
        assertEquals(GameEffect.RAISE_DIE_PLUS_4, value.effectiveEffect)
    }

    @Test fun `battle and cultivation values remain phase-distinct`() {
        val battle = ActionValue(CardPhase.BATTLE, GameEffect.GAIN_SUNLIGHT_TOKEN,
            "Sunlight", legal = true, calculatedBenefit = 3.5)
        assertEquals(CardPhase.BATTLE, battle.phase)
        assertEquals(3.5, battle.finalPreference)
    }

    @Test fun `illegal candidates are ineligible regardless of apparent benefit`() {
        val illegal = ActionValue(CardPhase.CULTIVATION, null,
            "No dice available", legal = false, calculatedBenefit = 1000.0)
        assertEquals(Double.NEGATIVE_INFINITY, illegal.finalPreference)
        assertThrows(IllegalArgumentException::class.java) { illegal.requireLegal() }
    }

    @Test fun `legacy adapter conserves integer scoring and explanatory adjustments`() {
        val original = PriorityScore(58, listOf(
            ScoreAdjustment(12, "capped raise"), ScoreAdjustment(-7, "timing")
        ))
        val value = ActionValue.fromPriorityScore(
            CardPhase.CULTIVATION, GameEffect.RAISE_DIE_PLUS_4, "root", original
        )
        assertEquals(original.total.toDouble(), value.finalPreference)
        assertEquals(12.0, value.contextualBonuses)
        assertEquals(7.0, value.contextualCosts)
        assertTrue(value.notes.any { it.contains("Legacy priority") })
    }

    @Test fun `invalid inputs cannot silently corrupt a decision`() {
        assertThrows(IllegalArgumentException::class.java) {
            ActionValue(CardPhase.BATTLE, null, "a", legal = true, calculatedBenefit = Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ActionValue(CardPhase.BATTLE, null, "a", legal = true, calculatedBenefit = 1.0, contextualCosts = -2.0)
        }
    }
}
