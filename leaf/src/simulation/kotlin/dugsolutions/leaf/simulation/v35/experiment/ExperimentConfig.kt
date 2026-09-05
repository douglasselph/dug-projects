package dugsolutions.leaf.simulation.v35.experiment

/** Reproducibility metadata for a future simulation batch. */
data class ExperimentConfig(
    val games: Int,
    val baseSeed: Long? = null
) {
    init {
        require(games > 0) { "Simulation experiment must run at least one game" }
    }
}
