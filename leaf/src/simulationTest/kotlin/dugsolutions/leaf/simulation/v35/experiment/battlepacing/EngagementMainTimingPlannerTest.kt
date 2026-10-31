package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

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
    @Test fun `two remaining Mains force spending in middle and low even if draw forecast favors low`() {
        val future = listOf(middle, bottom)
        val plan = EngagementMainTimingPlanner.planFromScores(
            middle, 2, future, mapOf(middle to -50.0, bottom to 500.0)
        )
        assertTrue(plan.spendNow)
        assertEquals(EngagementMainTimingDecision.Reason.MUST_SPEND_TO_USE_BOTH, plan.reason)
        assertEquals(future, plan.openRows)
        assertTrue(EngagementMainTimingPlanner.planFromScores(
            bottom, 1, listOf(bottom), mapOf(bottom to -100.0)
        ).spendNow)
    }

    @Test fun `high decision gets superseded by a new independent middle forecast`() {
        val highPlan = EngagementMainTimingPlanner.planFromScores(
            top, 2, rows, mapOf(top to 400.0, middle to 200.0, bottom to 50.0)
        )
        assertTrue(highPlan.spendNow)
        // After High resolves it is not considered and there is one token left.
        val updatedMiddle = EngagementMainTimingPlanner.planFromScores(
            middle, 1, listOf(middle, bottom), mapOf(middle to 0.0, bottom to 500.0)
        )
        assertFalse(updatedMiddle.spendNow)
        assertEquals(listOf(bottom), updatedMiddle.plannedRows)
        assertFalse(top in updatedMiddle.openRows)
        assertTrue(EngagementMainTimingPlanner.planFromScores(
            bottom, 1, listOf(bottom), mapOf(bottom to 500.0)
        ).spendNow)
    }

    @Test fun `missing expected Draw projection is transparently marked as provisional`() {
        val plan = EngagementMainTimingPlanner.planFromScores(top, 2, rows, emptyMap())
        assertTrue(plan.spendNow) // existing pilot fallback, not a tactical claim
        assertEquals(EngagementMainTimingDecision.Reason.NO_DRAW_PROJECTION, plan.reason)
        assertTrue(plan.projectedDrawScores.isEmpty())
        assertEquals(listOf(top, middle), plan.plannedRows)
    }

    @Test fun `timing choice never includes resolved row in projected plan`() {
        val plan = EngagementMainTimingPlanner.planFromScores(
            middle, 1, listOf(middle, bottom),
            mapOf(top to 10000.0, middle to 20.0, bottom to 10.0)
        )
        assertEquals(listOf(middle), plan.plannedRows)
        assertEquals(listOf(middle, bottom), plan.openRows)
    }
}
