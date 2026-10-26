package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class C05PreferenceUnitsTest {
    @Test fun `expected pips are not comparable to legacy points`() {
        val pips = PhasePreference(CardPhase.CULTIVATION, PreferenceUnit.CULTIVATION_EXPECTED_PIPS, 3.5)
        val legacy = PhasePreference(CardPhase.CULTIVATION, PreferenceUnit.LEGACY_PRIORITY_POINTS, 58.0)
        assertThrows(IllegalArgumentException::class.java) { pips.compareTo(legacy) }
    }

    @Test fun `different phases cannot be compared even for matching legacy units`() {
        val c = PhasePreference(CardPhase.CULTIVATION, PreferenceUnit.LEGACY_PRIORITY_POINTS, 50.0)
        val b = PhasePreference(CardPhase.BATTLE, PreferenceUnit.LEGACY_PRIORITY_POINTS, 50.0)
        assertThrows(IllegalArgumentException::class.java) { c.compareTo(b) }
    }

    @Test fun `phase incompatible direct units are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            PhasePreference(CardPhase.BATTLE, PreferenceUnit.CULTIVATION_EXPECTED_PIPS, 4.5)
        }
    }

    @Test fun `same phase same unit is comparable`() {
        val a = PhasePreference(CardPhase.BATTLE, PreferenceUnit.BATTLE_TACTICAL_UTILITY, -2.0)
        val b = PhasePreference(CardPhase.BATTLE, PreferenceUnit.BATTLE_TACTICAL_UTILITY, 3.0)
        assertTrue(a < b)
    }

    @Test fun `draw bridge preserves existing legacy calibration`() {
        assertEquals(45, LegacyPreferenceBridge.cultivationDraw(2.5).total)
        assertEquals(49, LegacyPreferenceBridge.cultivationDraw(3.5).total)
        assertEquals(77, LegacyPreferenceBridge.cultivationDraw(10.5).total)
        assertEquals(4, LegacyPreferenceBridge.battleTacticalAdjustment(3.6))
    }

    @Test fun `ActionValue rejects comparison across different valuation units`() {
        val old = ActionValue(CardPhase.CULTIVATION, null, "old", legal = true, calculatedBenefit = 58.0)
        val direct = ActionValue(CardPhase.CULTIVATION, null, "new", legal = true,
            calculatedBenefit = 4.0, unit = PreferenceUnit.CULTIVATION_EXPECTED_PIPS)
        assertThrows(IllegalArgumentException::class.java) { old.comparePreference(direct) }
    }
}
