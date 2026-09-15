package dugsolutions.leaf.v35.player.decision.baseline.scoring

/**
 * One legal Human Baseline choice before cross-system card influences are
 * applied.
 */
data class DecisionCandidate<T>(
    val choice: T,
    val score: PriorityScore,
    val tags: Set<DecisionTag> = emptySet(),
    val label: String = choice.toString()
) {
    init {
        require(label.isNotBlank()) {
            "Decision candidate label cannot be blank"
        }
    }
}
