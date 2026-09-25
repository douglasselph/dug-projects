package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.experiment.BatchRunResult
import kotlin.math.abs
import kotlin.math.sqrt

/** Aggregates cumulative prefixes of one compact Human Baseline batch. */
object BaselineCalibrationAggregator {
    private const val NEUTRAL_SEAT_SHARE = 0.25

    fun aggregate(
        spec: BaselineCalibrationSpec,
        batch: BatchRunResult
    ): BaselineCalibrationReport {
        require(batch.gamesCompleted >= spec.checkpoints.last()) {
            "Batch has ${batch.gamesCompleted} games but checkpoint ${spec.checkpoints.last()} was requested"
        }
        validateFourSeatSummaries(batch.summaries.take(spec.checkpoints.last()))

        return BaselineCalibrationReport(
            groveFingerprint = spec.groveFingerprint,
            totalGamesAvailable = batch.gamesCompleted,
            checkpoints = spec.checkpoints.map { games ->
                aggregateCheckpoint(batch.summaries.take(games))
            }
        )
    }

    private fun aggregateCheckpoint(summaries: List<GameSummary>): BaselineCalibrationCheckpoint {
        val seats = (0..3).map { seat ->
            val players = summaries.map { summary -> summary.players.single { it.seat == seat } }
            val winShare = players.sumOf { it.winShare } / summaries.size.toDouble()
            BaselineSeatMetrics(
                seat = seat,
                winShare = winShare,
                absoluteWinShareDeviationFromQuarter = abs(winShare - NEUTRAL_SEAT_SHARE),
                averageFinalVp = players.map { it.totalVp.toDouble() }.average()
            )
        }

        return BaselineCalibrationCheckpoint(
            games = summaries.size,
            seats = seats,
            maxAbsoluteWinShareDeviation = seats.maxOf { it.absoluteWinShareDeviationFromQuarter },
            sharedWinnerGameRate = summaries.count { it.winnerIds.size > 1 }.toDouble() / summaries.size,
            neutralSeatSamplingReference95HalfWidth =
                1.96 * sqrt(NEUTRAL_SEAT_SHARE * (1.0 - NEUTRAL_SEAT_SHARE) / summaries.size.toDouble())
        )
    }

    private fun validateFourSeatSummaries(summaries: List<GameSummary>) {
        require(summaries.isNotEmpty())
        summaries.forEachIndexed { index, summary ->
            require(summary.players.map { it.seat }.sorted() == listOf(0, 1, 2, 3)) {
                "Baseline game ${index + 1} must contain exactly physical seats 0..3"
            }
        }
    }
}
