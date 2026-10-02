package dugsolutions.leaf.simulation.v35.experiment.baseline

/** Stable human-readable cumulative report for manual calibration review. */
object BaselineCalibrationReportRenderer {
    fun render(report: BaselineCalibrationReport): String = buildString {
        appendLine("Human Baseline calibration")
        appendLine("Grove: ${report.groveFingerprint}")
        appendLine("Games available: ${report.totalGamesAvailable}")
        val seatCount = report.checkpoints.first().seats.size
        val winHeaders = (1..seatCount).joinToString(",") { "seat$it" }
        val vpHeaders = (1..seatCount).joinToString(",") { "avg_vp_$it" }
        appendLine("checkpoint,$winHeaders,max_deviation,$vpHeaders,shared_winner_rate,95pct_sampling_reference")
        report.checkpoints.forEach { checkpoint ->
            appendLine(
                buildList {
                    add(checkpoint.games.toString())
                    addAll(checkpoint.seats.map { pct(it.winShare) })
                    add(pct(checkpoint.maxAbsoluteWinShareDeviation))
                    addAll(checkpoint.seats.map { fmt(it.averageFinalVp) })
                    add(pct(checkpoint.sharedWinnerGameRate))
                    add("±${pct(checkpoint.neutralSeatSamplingReference95HalfWidth)}")
                }.joinToString(",")
            )
        }
        append("Sampling reference is context only; fractional shared wins are not exact binomial trials.")
    }

    private fun pct(value: Double): String = "%.2f%%".format(value * 100.0)
    private fun fmt(value: Double): String = "%.3f".format(value)
}
