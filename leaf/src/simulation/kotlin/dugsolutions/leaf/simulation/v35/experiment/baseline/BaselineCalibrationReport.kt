package dugsolutions.leaf.simulation.v35.experiment.baseline

/** Value-only cumulative calibration output for one exact Grove. */
data class BaselineCalibrationReport(
    val groveFingerprint: String,
    val totalGamesAvailable: Int,
    val checkpoints: List<BaselineCalibrationCheckpoint>
) {
    init {
        require(groveFingerprint.isNotBlank())
        require(totalGamesAvailable > 0)
        require(checkpoints.isNotEmpty())
    }
}

data class BaselineCalibrationCheckpoint(
    val games: Int,
    val seats: List<BaselineSeatMetrics>,
    val maxAbsoluteWinShareDeviation: Double,
    val sharedWinnerGameRate: Double,
    /**
     * Binomial-style 95% sampling reference half-width around 25%.
     * Context only: fractional shared wins are not exactly binomial trials.
     */
    val neutralSeatSamplingReference95HalfWidth: Double
) {
    init {
        require(games > 0)
        require(seats.map { it.seat }.sorted() == listOf(0, 1, 2, 3)) {
            "Baseline calibration requires physical seats 0..3"
        }
        require(maxAbsoluteWinShareDeviation >= 0.0)
        require(sharedWinnerGameRate in 0.0..1.0)
        require(neutralSeatSamplingReference95HalfWidth >= 0.0)
    }
}

data class BaselineSeatMetrics(
    /** Zero-based physical seat. */
    val seat: Int,
    val winShare: Double,
    val absoluteWinShareDeviationFromQuarter: Double,
    val averageFinalVp: Double
) {
    init {
        require(seat in 0..3)
        require(winShare in 0.0..1.0)
        require(absoluteWinShareDeviationFromQuarter >= 0.0)
    }
}
