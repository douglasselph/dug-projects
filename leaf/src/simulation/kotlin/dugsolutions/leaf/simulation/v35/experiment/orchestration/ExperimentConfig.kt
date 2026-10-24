package dugsolutions.leaf.simulation.v35.experiment.orchestration

/** Execution boundaries only. Rules, seeds and analysis belong to the specific experiment. */
data class ExperimentConfig(
    val firstSample: Int,
    val games: Int,
    val chunkSize: Int
) {
    init {
        require(firstSample >= 0) { "firstSample must not be negative" }
        require(games > 0) { "games must be positive" }
        require(chunkSize > 0) { "chunkSize must be positive" }
        require(firstSample.toLong() + games <= Int.MAX_VALUE.toLong()) { "sample range overflows Int" }
    }

    val endSampleExclusive: Int get() = (firstSample.toLong() + games).toInt()
}
