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

    /** Stable per-sample mechanical seed; shared configs therefore form matched runs. */
    fun mechanicalSeedAt(sample: Int): Long? = seedAt(baseSeed, sample)

    /** Stable per-sample strategy seed, independent from the mechanical stream. */
    fun strategySeedAt(sample: Int): Long? = seedAt(strategyBaseSeed, sample)

    private fun seedAt(base: Long?, sample: Int): Long? {
        require(sample in 0 until games) { "Sample index out of range: $sample" }
        return base?.plus(sample.toLong())
    }
}
