package dugsolutions.leaf.v35.player.decision.baseline.scoring

/** One line of a structured score explanation. */
data class ScoreExplanationLine(
    val amount: Int,
    val reason: String
) {
    init {
        require(reason.isNotBlank()) {
            "Score explanation reason cannot be blank"
        }
    }
}

/**
 * Structured explanation suitable for tests, Chronicle/debug output, or a
 * human-readable rendering without re-running the scoring logic.
 */
data class ScoreExplanation(
    val label: String?,
    val lines: List<ScoreExplanationLine>,
    val total: Int
) {
    fun render(): String =
        buildString {
            label?.takeIf { it.isNotBlank() }?.let {
                appendLine(it)
            }
            lines.forEachIndexed { index, line ->
                val displayedAmount =
                    if (index > 0 && line.amount > 0) {
                        "+${line.amount}"
                    } else {
                        line.amount.toString()
                    }
                appendLine("$displayedAmount  ${line.reason}")
            }
            append("Total  $total")
        }
}
