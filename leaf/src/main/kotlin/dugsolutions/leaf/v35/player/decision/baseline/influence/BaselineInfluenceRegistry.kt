package dugsolutions.leaf.v35.player.decision.baseline.influence

import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoreAdjustment
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/** Applies all cross-system influences supplied by the actor's owned cards. */
class BaselineInfluenceRegistry(
    private val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
) {
    fun adjustments(
        context: DecisionContext,
        candidate: DecisionCandidate<*>
    ): List<ScoreAdjustment> {
        if (candidate.tags.isEmpty() || context == DecisionContext.EMPTY) return emptyList()

        // Intentionally preserve duplicate owned cards. If two copies of a
        // card create the same synergy, each owned copy contributes it.
        return cardScorers.influencersForOwnedCards(context).flatMap { influencer ->
            influencer.adjustments(context, candidate)
        }
    }

    fun <T> score(
        context: DecisionContext,
        candidate: DecisionCandidate<T>
    ): ScoredChoice<T> =
        ScoredChoice.from(
            candidate = candidate,
            additionalAdjustments = adjustments(context, candidate)
        )
}
