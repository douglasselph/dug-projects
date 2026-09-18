package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.planned.TargetCard
import kotlin.test.Test
import kotlin.test.assertEquals

class CardExperimentAggregatorTest {
    @Test
    fun aggregate_reportsOverallAndSeatBalancedMetrics() {
        val observations = listOf(
            observation(seat = 0, won = true, winShare = 1.0, baselineWin = 0.0, vp = 30, baselineVp = 20.0, copies = 1, activations = 2),
            observation(seat = 1, won = false, winShare = 0.0, baselineWin = 0.5, vp = 18, baselineVp = 24.0, copies = 0, activations = 0),
            observation(seat = 0, won = false, winShare = 0.0, baselineWin = 0.5, vp = 22, baselineVp = 25.0, copies = 1, activations = 1),
            observation(seat = 1, won = true, winShare = 1.0, baselineWin = 0.0, vp = 28, baselineVp = 21.0, copies = 2, activations = 3)
        )

        val result = CardExperimentAggregator.aggregate(
            targetCard = TargetCard("Root_07_02"),
            targetCount = 2,
            numPlayers = 2,
            observations = observations
        )

        assertEquals(4, result.gamesCompleted)
        assertEquals(0.5, result.focusedWinnerRate)
        assertEquals(0.5, result.focusedWinShare)
        assertEquals(0.25, result.baselineAverageWinShare)
        assertEquals(0.25, result.winShareDeltaVsBaseline)
        assertEquals(24.5, result.focusedAverageVp)
        assertEquals(22.5, result.baselineAverageVp)
        assertEquals(2.0, result.averageVpDeltaVsBaseline)
        assertEquals(0.75, result.acquisitionGameRate)
        assertEquals(1.0, result.averageTargetCopiesPurchased)
        assertEquals(1.5, result.averageTargetActivations)
        assertEquals(2, result.bySeat.size)
        assertEquals(2, result.bySeat[0].gamesCompleted)
        assertEquals(2, result.bySeat[1].gamesCompleted)
    }

    private fun observation(
        seat: Int,
        won: Boolean,
        winShare: Double,
        baselineWin: Double,
        vp: Int,
        baselineVp: Double,
        copies: Int,
        activations: Int
    ) = CardFocusGameObservation(
        focusSeat = seat,
        focusWon = won,
        focusWinShare = winShare,
        baselineAverageWinShare = baselineWin,
        focusVp = vp,
        baselineAverageVp = baselineVp,
        targetPurchasedCopies = copies,
        targetGraftedCopiesAtEnd = copies,
        targetActivations = activations,
        targetPlantVp = copies * 2,
        battleStrikeVp = 2
    )
}
