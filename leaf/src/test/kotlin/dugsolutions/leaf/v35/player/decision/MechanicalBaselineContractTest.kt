package dugsolutions.leaf.v35.player.decision

import kotlin.test.Test
import kotlin.test.assertEquals

/** Temporary compatibility coverage for the pre-layer-clarification API. */
@Suppress("DEPRECATION")
class MechanicalBaselineCompatibilityTest {

    @Test
    fun `old mechanical baseline facade still maps to mechanical control`() {
        assertEquals(MechanicalControl.NAME, MechanicalBaseline.NAME)
        assertEquals(MechanicalControl.STRATEGY_LEVEL, MechanicalBaseline.STRATEGY_LEVEL)
        assertEquals(MechanicalControl.rules, MechanicalBaseline.rules)
        assertEquals(
            DecisionDirector.mechanicalControl()::class,
            DecisionDirector.mechanicalBaseline()::class
        )
    }
}
