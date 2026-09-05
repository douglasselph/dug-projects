package dugsolutions.leaf.v35.player.decision.baseline.scoring

/**
 * Human Baseline priority for one complete decision candidate.
 *
 * A priority is intentionally explainable: the base score is kept separate
 * from every contextual adjustment so simulations can later report not only
 * what was chosen, but why it outranked the alternatives.
 *
 * Priority is a strategy preference, not a measurement of a card's objective
 * game-design strength.
 */
data class PriorityScore(
    val base: Int,
    val adjustments: List<ScoreAdjustment> = emptyList()
) {
    val total: Int
        get() = base + adjustments.sumOf { it.amount }

    fun adjusted(
        amount: Int,
        reason: String
    ): PriorityScore =
        adjusted(
            ScoreAdjustment(
                amount = amount,
                reason = reason
            )
        )

    fun adjusted(
        adjustment: ScoreAdjustment
    ): PriorityScore =
        copy(
            adjustments = adjustments + adjustment
        )

    fun explanation(
        baseReason: String = "Base score"
    ): ScoreExplanation =
        ScoreExplanation(
            label = null,
            lines = listOf(
                ScoreExplanationLine(
                    amount = base,
                    reason = baseReason
                )
            ) + adjustments.map { adjustment ->
                ScoreExplanationLine(
                    amount = adjustment.amount,
                    reason = adjustment.reason
                )
            },
            total = total
        )
}
