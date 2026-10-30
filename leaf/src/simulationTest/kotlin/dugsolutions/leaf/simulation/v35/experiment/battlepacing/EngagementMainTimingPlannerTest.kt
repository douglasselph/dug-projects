package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EngagementMainTimingPlannerTest {
    private val top = StrikeRow.TOP
    private val middle = StrikeRow.MIDDLE
    private val bottom = StrikeRow.BOTTOM
    private val rows = listOf(top, middle, bottom)

    @Test fun `defer high when future two rows have more tactical promise`() {
        val scores = mapOf(top to 1.0, middle to 500.0, bottom to 300.0)
        assertFalse(EngagementMainTimingPlanner.shouldSpendInRow(top, 2, rows, scores))
        assertTrue(EngagementMainTimingPlanner.shouldSpendInRow(middle, 2, listOf(middle, bottom), scores))
        assertTrue(EngagementMainTimingPlanner.shouldSpendInRow(bottom, 1, listOf(bottom), scores))
    }

    @Test fun `reconsider after high and save final normal Main for low`() {
        val scores = mapOf(middle to 10.0, bottom to 505.0)
        assertFalse(EngagementMainTimingPlanner.shouldSpendInRow(middle, 1, listOf(middle, bottom), scores))
        assertTrue(EngagementMainTimingPlanner.shouldSpendInRow(bottom, 1, listOf(bottom), scores))
    }

    @Test fun `equal tactical benefit favors earlier resolution instead of forcing deferral`() {
        val scores = rows.associateWith { 10.0 }
        assertTrue(EngagementMainTimingPlanner.shouldSpendInRow(top, 2, rows, scores))
        assertTrue(EngagementMainTimingPlanner.shouldSpendInRow(middle, 1, listOf(middle, bottom), scores))
    }

    @Test fun `closed and inaccessible rows cannot spend token`() {
        val scores = rows.associateWith { 50.0 }
        assertFalse(EngagementMainTimingPlanner.shouldSpendInRow(top, 2, listOf(middle, bottom), scores))
    }
}
