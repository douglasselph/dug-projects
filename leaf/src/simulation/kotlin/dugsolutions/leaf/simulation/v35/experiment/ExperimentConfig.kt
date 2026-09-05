package dugsolutions.leaf.simulation.v35.experiment

/**
 * Reproducibility metadata for a future simulation batch.
 *
 * [baseSeed] is the mechanical game stream seed. [strategyBaseSeed] controls
 * strategy-only equal-score tie breaking. Keeping them independent allows A/B
 * strategy changes without silently perturbing later die rolls or shuffles.
 */
data class ExperimentConfig(
    val games: Int,
    val baseSeed: Long? = null,
    val strategyBaseSeed: Long? = baseSeed
) {
    init {
        require(games > 0) { "Simulation experiment must run at least one game" }
    }
}
