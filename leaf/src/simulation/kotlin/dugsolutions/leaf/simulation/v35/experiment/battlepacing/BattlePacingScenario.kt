package dugsolutions.leaf.simulation.v35.experiment.battlepacing

/**
 * Selection on *untouched*, legal, canonical pre-Battle openings. No resource
 * injection, post-treatment filtering, or post-Battle outcomes are consulted.
 * Filtered cohorts estimate conditional effects, not population-wide effects.
 */
internal enum class BattlePacingScenario(val cliName: String) {
    NATURAL("natural"),
    RESOURCE_RICH("resource-rich"),
    ASYMMETRIC("asymmetric");

    fun accepts(initialSupports: List<Int>): Boolean {
        require(initialSupports.size in 2..4 && initialSupports.all { it >= 0 })
        val largest = initialSupports.max()
        val smallest = initialSupports.min()
        return when (this) {
            NATURAL -> true
            // At least one player has substantial Support resources.
            RESOURCE_RICH -> largest >= 8
            // Players differ substantially; these are still naturally occurring inventories.
            ASYMMETRIC -> largest >= 8 && largest - smallest >= 5
        }
    }

    companion object {
        fun parse(value: String): BattlePacingScenario = entries.firstOrNull { it.cliName == value }
            ?: error("Unknown scenario '$value'; choose ${entries.joinToString("|") { it.cliName }}")
    }
}

internal data class BattlePacingSelection(
    val supportInventory: List<Int>,
    val initialVP: List<Int>
) {
    init {
        require(supportInventory.size in 2..4 && supportInventory.size == initialVP.size)
        require(supportInventory.all { it >= 0 })
    }

    val minimumSupports: Int get() = supportInventory.min()
    val maximumSupports: Int get() = supportInventory.max()
    val supportGap: Int get() = maximumSupports - minimumSupports
    val vpGap: Int get() = initialVP.max() - initialVP.min()
}
