package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.cultivation.*

/**
 * Expert scores for ALL legal actions remain unchanged except the additive Compost preference.
 * One preference point is 1% of this decision's learned-score range (minimum range 1.0).
 * Thus this is intentionally NOT a Human Baseline score unit or a probability.
 */
class CompostPreferenceCultivationPolicy(
    weights: LearnedCultivationMainWeights,
    private val compostPreference: Double
) : CultivationMainPolicy {
    init { require(compostPreference.isFinite()) }
    private val scorer = LinearCultivationMainActionScorer(weights)

    override fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction {
        val scores = request.legalActions.map { action ->
            scorer.score(CultivationMainFeatureExtractor.extract(request, action)).total
        }
        val spread = (scores.maxOrNull()!! - scores.minOrNull()!!).coerceAtLeast(1.0)
        val bonus = compostPreference * spread / 100.0
        return request.legalActions.indices.maxByOrNull { i ->
            scores[i] + if (request.isCompost(request.legalActions[i])) bonus else 0.0
        }!!.let(request.legalActions::get)
    }

    private fun ChooseCultivationMainActionRequest.isCompost(action: CultivationMainAction): Boolean {
        val effect = when (action) {
            CultivationMainAction.RoundEffect1 -> observation.firstRoundEffect
            CultivationMainAction.RoundEffect2 -> observation.secondRoundEffect
            else -> null
        }
        return effect == GameEffect.UPGRADE_DIE_FROM_HAND ||
            effect == GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS ||
            effect == GameEffect.UPGRADE_DIE_AND_USE_NOW
    }
}
