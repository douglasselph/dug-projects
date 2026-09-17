package dugsolutions.leaf.simulation.v35.strategy.planned

import dugsolutions.leaf.simulation.v35.strategy.StrategyLevel
import dugsolutions.leaf.simulation.v35.strategy.StrategyProfile
import dugsolutions.leaf.v35.player.decision.DecisionArea
import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlannedBaselineStrategyTest {
    @Test
    fun plannedProfile_remainsHumanBaselineInEveryDecisionArea() {
        val plan = CreaturePlan.of("Root_07_02" to 2)
        val profile = StrategyProfile.plannedBaseline(plan)

        assertTrue(profile.name.startsWith("Planned Baseline"))
        DecisionArea.entries.forEach { area ->
            assertEquals(StrategyLevel.HUMAN_BASELINE, profile.levelFor(area))
        }
        assertIs<HumanBaselineBuyStrategy>(profile.createDirector().buy)
    }
}
