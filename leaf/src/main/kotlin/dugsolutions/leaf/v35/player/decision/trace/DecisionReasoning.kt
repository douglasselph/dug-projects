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
 * Typed score breakdown for one legal alternative in a Human Baseline decision.
 *
 * Alternatives are produced only when a reasoning sink is enabled. They retain
 * the score components already known by the scoring system without retaining
 * mutable gameplay objects.
 */
data class DecisionReasoningAlternative(
    val choiceLabel: String,
    val baseScore: Int,
    val adjustments: List<DecisionReasoningAdjustment>,
    val total: Int,
    val selected: Boolean,
    val observations: Map<String, String> = emptyMap()
) {
    init {
        require(choiceLabel.isNotBlank()) {
            "Decision reasoning alternative choice label cannot be blank"
        }
        require(total == baseScore + adjustments.sumOf { it.amount }) {
            "Decision reasoning alternative total does not match base + adjustments: " +
                "base=$baseScore adjustments=${adjustments.sumOf { it.amount }} total=$total"
        }
    }
}

/**
 * Strategy-facing debug snapshot for one scored decision.
 *
 * The selected-choice fields are retained for existing Chronicle/debug users.
 * [alternatives] contains every legal scored alternative, including the
 * selected choice, so research tooling can explain both selection and
 * rejection without parsing rendered Chronicle text.
 *
 * This deliberately contains only immutable scalar/debug data. Normal games
 * use [DecisionReasoningSink.NONE], so the full alternative snapshot is not
 * constructed for large simulations unless diagnostics are explicitly enabled.
 */
data class DecisionReasoning(
    val choiceLabel: String,
    val baseScore: Int,
    val adjustments: List<DecisionReasoningAdjustment>,
    val total: Int,
    val alternatives: List<DecisionReasoningAlternative> = emptyList(),
    val observations: Map<String, String> = emptyMap()
) {
    init {
        require(choiceLabel.isNotBlank()) {
            "Decision reasoning choice label cannot be blank"
        }
        require(total == baseScore + adjustments.sumOf { it.amount }) {
            "Decision reasoning total does not match base + adjustments: " +
                "base=$baseScore adjustments=${adjustments.sumOf { it.amount }} total=$total"
        }
        if (alternatives.isNotEmpty()) {
            require(alternatives.count { it.selected } == 1) {
                "Decision reasoning alternatives must contain exactly one selected choice"
            }
            val selected = alternatives.single { it.selected }
            require(
                selected.choiceLabel == choiceLabel &&
                    selected.baseScore == baseScore &&
                    selected.adjustments == adjustments &&
                    selected.total == total &&
                    selected.observations == observations
            ) {
                "Selected alternative must match the selected decision reasoning fields"
            }
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
