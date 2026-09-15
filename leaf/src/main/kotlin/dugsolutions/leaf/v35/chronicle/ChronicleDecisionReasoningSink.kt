package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.DecisionScoreAdjustmentSnapshot
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoning
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink

/** Records optional strategy score explanations into the owning game's Chronicle. */
class ChronicleDecisionReasoningSink(
    private val chronicle: Chronicle,
    private val playerId: PlayerId
) : DecisionReasoningSink {

    override fun record(reasoning: DecisionReasoning) {
        chronicle.record(
            Moment.DecisionReasoning(
                playerId = playerId,
                choiceLabel = reasoning.choiceLabel,
                baseScore = reasoning.baseScore,
                adjustments = reasoning.adjustments.map { adjustment ->
                    DecisionScoreAdjustmentSnapshot(
                        amount = adjustment.amount,
                        reason = adjustment.reason
                    )
                },
                total = reasoning.total
            )
        )
    }
}
