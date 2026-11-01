package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals

class BattlePacingTimingModelTest {
    @Test fun `equivalent one-row event sequences receive equivalent time under same assumptions`() {
        val player = PlayerId(1)
        fun observed(ruleSet: PacingRuleSet, row: StrikeRow?): BattlePacingObserved {
            val step = BattlePacingStepObserved(
                playerId = player,
                legalMainCount = 1,
                legalSupportCount = 0,
                unresolvedStrikes = 1,
                actionKind = "MAIN_DRAW",
                pass = false,
                beforeRows = emptyList(), afterRows = emptyList(),
                beforePlants = emptyList(), afterPlants = emptyList(),
                beforeResources = emptyList(), afterResources = emptyList(),
                chronicleEntries = emptyList()
            )
            val window = BattlePacingWindowObserved(
                index = 1, playerId = player, stage = "TEST", row = row,
                unresolvedStrikes = 1, steps = listOf(step), emptyOpportunity = false,
                passCount = 0, beforeRows = emptyList(), afterRows = emptyList(),
                beforePlants = emptyList(), afterPlants = emptyList()
            )
            return BattlePacingObserved(
                ruleSet = ruleSet, players = listOf(player), windows = listOf(window),
                initialPlacedDice = 0, strikeCount = 1, woundCount = 0,
                doomDiceCount = 0, cleanupDiceCount = 0, returnedCritters = 0,
                remainingSupportInventory = mapOf(player to 0)
            )
        }

        val profile = BattleTimingProfiles.defaults.single { it.name == "typical" }
        val baseline = estimateBattleTime(observed(PacingRuleSet.A_BASELINE, null), profile)
        val candidate = estimateBattleTime(observed(PacingRuleSet.B_CANDIDATE, StrikeRow.TOP), profile)
        assertEquals(baseline.totalSeconds, candidate.totalSeconds, 1e-9)
        assertEquals(baseline.totalDecisionSeconds, candidate.totalDecisionSeconds, 1e-9)
        assertEquals(baseline.totalExecutionSeconds, candidate.totalExecutionSeconds, 1e-9)
    }
}
