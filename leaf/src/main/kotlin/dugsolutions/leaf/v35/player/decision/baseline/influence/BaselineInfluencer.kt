package dugsolutions.leaf.v35.player.decision.baseline.influence

import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Cross-system modifier contributed by an owned card.
 *
 * Implementations inspect semantic candidate tags rather than concrete
 * strategy classes, so the source of a synergy remains in the card's own
 * Human Baseline scorer.
 */
interface BaselineInfluencer {
    fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment>
}
