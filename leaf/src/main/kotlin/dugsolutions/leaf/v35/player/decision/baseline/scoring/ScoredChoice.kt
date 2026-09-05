package dugsolutions.leaf.v35.player.decision.baseline.scoring

/** One legal action+target candidate together with its Human Baseline score. */
data class ScoredChoice<T>(
    val choice: T,
    val score: PriorityScore
) {
    fun explanation(
        label: String = choice.toString(),
        baseReason: String = "Base score"
    ): ScoreExplanation =
        score.explanation(baseReason).copy(
            label = label
        )
}
