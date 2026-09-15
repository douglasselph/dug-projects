package dugsolutions.leaf.v35.player.decision.trace

/** One contextual adjustment contributing to a recorded strategy score. */
data class DecisionReasoningAdjustment(
    val amount: Int,
    val reason: String
) {
    init {
        require(reason.isNotBlank()) {
            "Decision reasoning adjustment reason cannot be blank"
        }
    }
}

/**
 * Strategy-facing debug snapshot for one selected decision.
 *
 * This deliberately contains only immutable scalar/debug data. It can be sent
 * to Chronicle, printed by a debugger, or ignored entirely by large
 * simulations without retaining mutable gameplay objects.
 */
data class DecisionReasoning(
    val choiceLabel: String,
    val baseScore: Int,
    val adjustments: List<DecisionReasoningAdjustment>,
    val total: Int
) {
    init {
        require(choiceLabel.isNotBlank()) {
            "Decision reasoning choice label cannot be blank"
        }
        require(total == baseScore + adjustments.sumOf { it.amount }) {
            "Decision reasoning total does not match base + adjustments: " +
                "base=$baseScore adjustments=${adjustments.sumOf { it.amount }} total=$total"
        }
    }

    fun render(): String =
        buildString {
            appendLine(choiceLabel)
            appendLine("$baseScore  Base score")
            adjustments.forEach { adjustment ->
                val displayedAmount =
                    if (adjustment.amount > 0) {
                        "+${adjustment.amount}"
                    } else {
                        adjustment.amount.toString()
                    }
                appendLine("$displayedAmount  ${adjustment.reason}")
            }
            append("Total  $total")
        }
}

/** Optional destination for decision reasoning. */
fun interface DecisionReasoningSink {
    fun record(reasoning: DecisionReasoning)

    companion object {
        val NONE: DecisionReasoningSink = DecisionReasoningSink { }
    }
}
