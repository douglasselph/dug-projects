package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattlePacingScenarioTest {
    @Test fun `natural accepts any legal opening`() {
        assertTrue(BattlePacingScenario.NATURAL.accepts(listOf(0, 0)))
        assertTrue(BattlePacingScenario.NATURAL.accepts(listOf(3, 12, 0, 2)))
    }

    @Test fun `resource rich requires at least one eight resource player`() {
        assertFalse(BattlePacingScenario.RESOURCE_RICH.accepts(listOf(7, 7, 7)))
        assertTrue(BattlePacingScenario.RESOURCE_RICH.accepts(listOf(7, 8, 1)))
    }

    @Test fun `asymmetric requires both eight resource player and five resource gap`() {
        assertFalse(BattlePacingScenario.ASYMMETRIC.accepts(listOf(3, 7, 2)))
        assertFalse(BattlePacingScenario.ASYMMETRIC.accepts(listOf(6, 8, 7)))
        assertTrue(BattlePacingScenario.ASYMMETRIC.accepts(listOf(3, 8, 4)))
        assertTrue(BattlePacingScenario.ASYMMETRIC.accepts(listOf(2, 10, 6, 1)))
    }

    @Test fun `profiles describe opening inventories only`() {
        val opening = BattlePacingSelection(listOf(2, 10, 6, 1), listOf(11, 3, 8, 7))
        assertEquals(1, opening.minimumSupports)
        assertEquals(10, opening.maximumSupports)
        assertEquals(9, opening.supportGap)
        assertEquals(8, opening.vpGap)
    }
}
