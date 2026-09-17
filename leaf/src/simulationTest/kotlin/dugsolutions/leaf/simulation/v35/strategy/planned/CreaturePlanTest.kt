package dugsolutions.leaf.simulation.v35.strategy.planned

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreaturePlanTest {
    @Test
    fun exactTargetCounts_areRetainedByStableCardName() {
        val plan = CreaturePlan.of(
            "Root_07_02" to 2,
            "Flower_14_01" to 1
        )

        assertEquals(2, plan.targetCount(TargetCard("Root_07_02")))
        assertEquals(1, plan.targetCount(TargetCard("Flower_14_01")))
        assertEquals(0, plan.targetCount(TargetCard("Root_05_04")))
        assertTrue(plan.isTarget(TargetCard("Root_07_02")))
    }

    @Test
    fun targetCounts_mustBePositive() {
        assertFailsWith<IllegalArgumentException> {
            CreaturePlan.of("Root_07_02" to 0)
        }
    }
}
