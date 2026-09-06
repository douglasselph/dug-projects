package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.GameProgressView
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DevelopmentTargetHeuristicsTest {

    @Test
    fun standardCultivationCurve_reachesSixPlantsAndDicePower54ByRoundEight() {
        val config = DevelopmentTargetConfig()

        assertEquals(1, DevelopmentTargetHeuristics.targetPlantCount(1, config))
        assertEquals(33, DevelopmentTargetHeuristics.targetDicePower(1, config))
        assertEquals(6, DevelopmentTargetHeuristics.targetPlantCount(8, config))
        assertEquals(54, DevelopmentTargetHeuristics.targetDicePower(8, config))
    }

    @Test
    fun currentCultivationRound_isTargetMilestone() {
        val progress = GameProgressView.EMPTY.copy(
            cultivationRoundsCompleted = 3,
            totalCultivationRounds = 8,
            currentCultivationRoundNumber = 4
        )

        assertEquals(
            4,
            DevelopmentTargetHeuristics.cultivationMilestone(
                progress,
                RoundCardType.CULTIVATION
            )
        )
    }

    @Test
    fun battle_usesEndOfCultivationTargetForOrderedGame() {
        val progress = GameProgressView.EMPTY.copy(
            cultivationRoundsCompleted = 8,
            totalCultivationRounds = 8,
            currentBattleRoundNumber = 1
        )

        assertEquals(
            8,
            DevelopmentTargetHeuristics.cultivationMilestone(
                progress,
                RoundCardType.BATTLE
            )
        )
    }

    @Test
    fun assess_reportsDeficitsAgainstCurrentPlayerBoard() {
        val context = DecisionContext.EMPTY.copy(
            phase = RoundCardType.CULTIVATION,
            progress = GameProgressView.EMPTY.copy(
                cultivationRoundsCompleted = 3,
                totalCultivationRounds = 8,
                currentCultivationRoundNumber = 4
            )
        )

        val target = DevelopmentTargetHeuristics.assess(context)

        assertEquals(4, target.cultivationMilestone)
        assertEquals(3, target.targetPlantCount)
        assertEquals(42, target.targetDicePower)
        assertEquals(3, target.plantDeficit)
        assertEquals(42, target.dicePowerDeficit)
    }
}
