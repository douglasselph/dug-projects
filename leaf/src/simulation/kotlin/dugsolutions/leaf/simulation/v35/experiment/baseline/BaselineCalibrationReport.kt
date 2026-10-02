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
     * Binomial-style 95% sampling reference half-width around the neutral 1/N seat share.
     * Context only: fractional shared wins are not exactly binomial trials.
     */
    val neutralSeatSamplingReference95HalfWidth: Double
) {
    init {
        require(games > 0)
        require(seats.size in 2..4 && seats.map { it.seat }.sorted() == (0 until seats.size).toList()) {
            "Baseline calibration requires contiguous physical seats for 2 to 4 players"
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
    val absoluteWinShareDeviationFromNeutral: Double,
    val averageFinalVp: Double
) {
    init {
        require(seat in 0..3)
        require(winShare in 0.0..1.0)
        require(absoluteWinShareDeviationFromNeutral >= 0.0)
    }
}
