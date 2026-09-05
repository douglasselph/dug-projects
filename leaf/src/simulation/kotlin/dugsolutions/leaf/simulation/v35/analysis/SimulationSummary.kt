package dugsolutions.leaf.simulation.v35.analysis

/** Small neutral result shape for future aggregate experiment reporting. */
data class SimulationSummary(
    val gamesCompleted: Int,
    val winsByProfile: Map<String, Int>
) {
    init {
        require(gamesCompleted >= 0) { "Completed game count cannot be negative" }
        require(winsByProfile.values.all { it >= 0 }) { "Win counts cannot be negative" }
    }
}
