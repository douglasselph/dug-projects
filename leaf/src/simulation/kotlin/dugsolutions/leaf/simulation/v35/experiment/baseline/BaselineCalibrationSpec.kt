package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.ExperimentConfig
import dugsolutions.leaf.v35.plant.domain.PlantCard

/**
 * Reproducible specification for one Human Baseline calibration run.
 *
 * Checkpoints are cumulative prefixes of the same deterministic run. The exact
 * nine-card Grove is part of the specification so calibration results cannot
 * silently be compared across different Plant environments.
 */
data class BaselineCalibrationSpec(
    val selectedPlantCards: List<PlantCard>,
    val games: Int,
    val baseSeed: Long? = null,
    val strategyBaseSeed: Long? = baseSeed,
    val checkpoints: List<Int> = listOf(games)
) {
    init {
        require(selectedPlantCards.size == 9) {
            "Baseline calibration requires exactly nine selected Plant cards"
        }
        require(selectedPlantCards.map { it.name }.distinct().size == selectedPlantCards.size) {
            "Baseline calibration Plant cards must have distinct stable names"
        }
        require(games > 0) { "Baseline calibration must run at least one game" }
        require(checkpoints.isNotEmpty()) { "Baseline calibration requires at least one checkpoint" }
        require(checkpoints == checkpoints.distinct().sorted()) {
            "Baseline calibration checkpoints must be unique and increasing"
        }
        require(checkpoints.all { it in 1..games }) {
            "Baseline calibration checkpoints must be between 1 and games=$games: $checkpoints"
        }
    }

    /** Canonical composition fingerprint; Grove list order does not affect identity. */
    val groveFingerprint: String = selectedPlantCards.map { it.name }.sorted().joinToString("|")

    fun experimentConfig(): ExperimentConfig = ExperimentConfig(
        games = games,
        baseSeed = baseSeed,
        strategyBaseSeed = strategyBaseSeed
    )
}
