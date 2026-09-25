package dugsolutions.leaf.simulation.v35.experiment

import dugsolutions.leaf.simulation.v35.analysis.GameSummary

/** Compact retained output from a high-volume complete-game batch. */
data class BatchRunResult(
    val matchupName: String,
    val summaries: List<GameSummary>
) {
    init {
        require(matchupName.isNotBlank())
        require(summaries.isNotEmpty()) { "Batch result requires at least one completed game" }
    }

    val gamesCompleted: Int
        get() = summaries.size
}
