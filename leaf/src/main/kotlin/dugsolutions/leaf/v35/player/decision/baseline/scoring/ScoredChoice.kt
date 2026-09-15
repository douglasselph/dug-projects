package dugsolutions.leaf.v35.player.decision.baseline.scoring

/** One legal action+target candidate together with its Human Baseline score. */
data class ScoredChoice<T>(
    val choice: T,
    val score: PriorityScore,
    val label: String = choice.toString()
) {
    init {
        require(label.isNotBlank()) {
            "Scored choice label cannot be blank"
        }
    }

    fun explanation(
        label: String = this.label,
        baseReason: String = "Base score"
    ): ScoreExplanation =
        score.explanation(baseReason).copy(
            label = label
        )

    companion object {
        /** Preserve the candidate's base reasoning and append global influences. */
        fun <T> from(
            candidate: DecisionCandidate<T>,
            additionalAdjustments: List<ScoreAdjustment> = emptyList()
        ): ScoredChoice<T> =
            ScoredChoice(
                choice = candidate.choice,
                score = candidate.score.copy(
                    adjustments = candidate.score.adjustments + additionalAdjustments
                ),
                label = candidate.label
            )
    }
}
