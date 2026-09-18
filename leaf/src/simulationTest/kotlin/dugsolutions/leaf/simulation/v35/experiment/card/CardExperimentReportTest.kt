package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.planned.TargetCard
import kotlin.test.Test
import kotlin.test.assertContains

class CardExperimentReportTest {
    @Test
    fun render_containsCoreExperimentMetrics() {
        val result = CardExperimentResult(
            targetCard = TargetCard("Flower_17_04"),
            targetCount = 1,
            numPlayers = 2,
            gamesCompleted = 20,
            focusedWinnerRate = 0.55,
            focusedWinShare = 0.525,
            baselineAverageWinShare = 0.475,
            winShareDeltaVsBaseline = 0.05,
            focusedAverageVp = 26.0,
            baselineAverageVp = 24.5,
            averageVpDeltaVsBaseline = 1.5,
            acquisitionGameRate = 0.8,
            averageTargetCopiesPurchased = 0.85,
            averageTargetCopiesAtEnd = 0.75,
            averageTargetActivations = 1.4,
            averageTargetPlantVp = 2.1,
            averageBattleStrikeVp = 3.0,
            bySeat = listOf(
                seat(0), seat(1)
            )
        )

        val report = CardExperimentReport.render(result)

        assertContains(report, "Flower_17_04")
        assertContains(report, "Games: 20")
        assertContains(report, "Win-share delta: +5.00%")
        assertContains(report, "Acquisition game rate: 80.00%")
        assertContains(report, "Avg activations: 1.400")
    }

    private fun seat(index: Int) = CardSeatResult(
        seat = index,
        gamesCompleted = 10,
        focusedWinnerRate = 0.5,
        focusedWinShare = 0.5,
        focusedAverageVp = 25.0,
        baselineAverageVp = 24.0,
        averageVpDeltaVsBaseline = 1.0,
        acquisitionGameRate = 0.8,
        averageTargetCopiesPurchased = 0.8,
        averageTargetActivations = 1.0
    )
}
