package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.planned.TargetCard

/** Aggregate metrics for one focused-card experiment. */
data class CardExperimentResult(
    val targetCard: TargetCard,
    val targetCount: Int,
    val numPlayers: Int,
    val gamesCompleted: Int,
    val focusedWinnerRate: Double,
    val focusedWinShare: Double,
    val baselineAverageWinShare: Double,
    val winShareDeltaVsBaseline: Double,
    val focusedAverageVp: Double,
    val baselineAverageVp: Double,
    val averageVpDeltaVsBaseline: Double,
    val acquisitionGameRate: Double,
    val averageTargetCopiesPurchased: Double,
    val averageTargetCopiesAtEnd: Double,
    val averageTargetActivations: Double,
    val averageTargetPlantVp: Double,
    val averageBattleStrikeVp: Double,
    val bySeat: List<CardSeatResult>
) {
    init {
        require(targetCount > 0)
        require(numPlayers in 2..4)
        require(gamesCompleted > 0)
        require(bySeat.size == numPlayers) {
            "Expected one seat result per player: seats=${bySeat.size}, players=$numPlayers"
        }
    }
}

/** Same core metrics split out by the focused player's seat. */
data class CardSeatResult(
    val seat: Int,
    val gamesCompleted: Int,
    val focusedWinnerRate: Double,
    val focusedWinShare: Double,
    val focusedAverageVp: Double,
    val baselineAverageVp: Double,
    val averageVpDeltaVsBaseline: Double,
    val acquisitionGameRate: Double,
    val averageTargetCopiesPurchased: Double,
    val averageTargetActivations: Double
)
