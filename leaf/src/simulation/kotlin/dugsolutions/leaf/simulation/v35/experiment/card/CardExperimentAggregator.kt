package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.simulation.v35.strategy.planned.TargetCard

/** Streaming-friendly aggregation of focused-card game observations. */
internal object CardExperimentAggregator {
    fun aggregate(
        targetCard: TargetCard,
        targetCount: Int,
        numPlayers: Int,
        observations: List<CardFocusGameObservation>
    ): CardExperimentResult {
        require(observations.isNotEmpty()) { "Cannot aggregate an empty card experiment" }

        val bySeat = (0 until numPlayers).map { seat ->
            val games = observations.filter { it.focusSeat == seat }
            require(games.isNotEmpty()) { "No card-experiment games recorded for seat $seat" }
            seatResult(seat, games)
        }

        return CardExperimentResult(
            targetCard = targetCard,
            targetCount = targetCount,
            numPlayers = numPlayers,
            gamesCompleted = observations.size,
            focusedWinnerRate = observations.averageOf { if (it.focusWon) 1.0 else 0.0 },
            focusedWinShare = observations.averageOf { it.focusWinShare },
            baselineAverageWinShare = observations.averageOf { it.baselineAverageWinShare },
            winShareDeltaVsBaseline = observations.averageOf { it.winShareDeltaVsBaseline },
            focusedAverageVp = observations.averageOf { it.focusVp.toDouble() },
            baselineAverageVp = observations.averageOf { it.baselineAverageVp },
            averageVpDeltaVsBaseline = observations.averageOf { it.vpDeltaVsBaseline },
            acquisitionGameRate = observations.averageOf {
                if (it.targetPurchasedCopies > 0) 1.0 else 0.0
            },
            averageTargetCopiesPurchased = observations.averageOf {
                it.targetPurchasedCopies.toDouble()
            },
            averageTargetCopiesAtEnd = observations.averageOf {
                it.targetGraftedCopiesAtEnd.toDouble()
            },
            averageTargetActivations = observations.averageOf {
                it.targetActivations.toDouble()
            },
            averageTargetPlantVp = observations.averageOf { it.targetPlantVp.toDouble() },
            averageBattleStrikeVp = observations.averageOf { it.battleStrikeVp.toDouble() },
            bySeat = bySeat
        )
    }

    private fun seatResult(
        seat: Int,
        games: List<CardFocusGameObservation>
    ): CardSeatResult =
        CardSeatResult(
            seat = seat,
            gamesCompleted = games.size,
            focusedWinnerRate = games.averageOf { if (it.focusWon) 1.0 else 0.0 },
            focusedWinShare = games.averageOf { it.focusWinShare },
            focusedAverageVp = games.averageOf { it.focusVp.toDouble() },
            baselineAverageVp = games.averageOf { it.baselineAverageVp },
            averageVpDeltaVsBaseline = games.averageOf { it.vpDeltaVsBaseline },
            acquisitionGameRate = games.averageOf {
                if (it.targetPurchasedCopies > 0) 1.0 else 0.0
            },
            averageTargetCopiesPurchased = games.averageOf {
                it.targetPurchasedCopies.toDouble()
            },
            averageTargetActivations = games.averageOf {
                it.targetActivations.toDouble()
            }
        )

    private inline fun <T> List<T>.averageOf(selector: (T) -> Double): Double =
        sumOf(selector) / size.toDouble()
}
