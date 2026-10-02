package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.experiment.BatchRunResult
import kotlin.math.abs
import kotlin.math.sqrt

/** Aggregates cumulative prefixes of one compact Human Baseline batch. */
object BaselineCalibrationAggregator {
    fun aggregate(
        spec: BaselineCalibrationSpec,
        batch: BatchRunResult
    ): BaselineCalibrationReport {
        require(batch.gamesCompleted >= spec.checkpoints.last()) {
            "Batch has ${batch.gamesCompleted} games but checkpoint ${spec.checkpoints.last()} was requested"
        }
        validateSeatSummaries(batch.summaries.take(spec.checkpoints.last()))

        return BaselineCalibrationReport(
            groveFingerprint = spec.groveFingerprint,
            totalGamesAvailable = batch.gamesCompleted,
            checkpoints = spec.checkpoints.map { games ->
                aggregateCheckpoint(batch.summaries.take(games))
            }
        )
    }

    private fun aggregateCheckpoint(summaries: List<GameSummary>): BaselineCalibrationCheckpoint {
        val playerCount = summaries.first().players.size
        val neutralSeatShare = 1.0 / playerCount
        val seats = (0 until playerCount).map { seat ->
            val players = summaries.map { summary -> summary.players.single { it.seat == seat } }
            val winShare = players.sumOf { it.winShare } / summaries.size.toDouble()
            BaselineSeatMetrics(
                seat = seat,
                winShare = winShare,
                absoluteWinShareDeviationFromNeutral = abs(winShare - neutralSeatShare),
                averageFinalVp = players.map { it.totalVp.toDouble() }.average()
            )
        }

        return BaselineCalibrationCheckpoint(
            games = summaries.size,
            seats = seats,
            maxAbsoluteWinShareDeviation = seats.maxOf { it.absoluteWinShareDeviationFromNeutral },
            sharedWinnerGameRate = summaries.count { it.winnerIds.size > 1 }.toDouble() / summaries.size,
            neutralSeatSamplingReference95HalfWidth =
                1.96 * sqrt(neutralSeatShare * (1.0 - neutralSeatShare) / summaries.size.toDouble())
        )
    }

    private fun validateSeatSummaries(summaries: List<GameSummary>) {
        require(summaries.isNotEmpty())
        val playerCount = summaries.first().players.size
        require(playerCount in 2..4) { "Baseline calibration requires 2 to 4 players, got $playerCount" }
        val expectedSeats = (0 until playerCount).toList()
        summaries.forEachIndexed { index, summary ->
            require(summary.players.map { it.seat }.sorted() == expectedSeats) {
                "Baseline game ${index + 1} must contain exactly physical seats ${expectedSeats.joinToString()}"
            }
        }
    }
}
