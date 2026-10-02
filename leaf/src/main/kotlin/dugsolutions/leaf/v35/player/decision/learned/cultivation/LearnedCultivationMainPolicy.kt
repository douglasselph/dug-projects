package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationMainActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainPolicy

/** Learned high-level Cultivation Main selection only; all effect targets remain Human Baseline. */
class LearnedCultivationMainPolicy(
    weights: LearnedCultivationMainWeights
) : CultivationMainPolicy {
    private val scorer = LinearCultivationMainActionScorer(weights)

    override fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction {
        // Stable legal-action order is the deterministic tie breaker.
        return request.legalActions.maxByOrNull { action ->
            scorer.score(CultivationMainFeatureExtractor.extract(request, action)).total
        } ?: error("Cultivation Main learner received no legal actions")
    }
}
