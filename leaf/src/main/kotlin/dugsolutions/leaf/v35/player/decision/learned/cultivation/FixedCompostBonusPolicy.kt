package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.cultivation.*

/** Fixed additive adjustment in native learned-score units. Applied during training AND evaluation. */
class FixedCompostBonusPolicy(
    weights: LearnedCultivationMainWeights,
    val bonus: Double
) : CultivationMainPolicy {
    init { require(bonus.isFinite()) }
    private val scorer = LinearCultivationMainActionScorer(weights)
    override fun chooseMainAction(request: ChooseCultivationMainActionRequest): CultivationMainAction =
        request.legalActions.maxByOrNull { action ->
            val base = scorer.score(CultivationMainFeatureExtractor.extract(request, action)).total
            base + if (isCompost(request, action)) bonus else 0.0
        } ?: error("No legal Cultivation Main actions")

    private fun isCompost(request: ChooseCultivationMainActionRequest, action: CultivationMainAction): Boolean {
        val effect = when (action) {
            CultivationMainAction.RoundEffect1 -> request.observation.firstRoundEffect
            CultivationMainAction.RoundEffect2 -> request.observation.secondRoundEffect
            else -> null
        }
        return effect == GameEffect.UPGRADE_DIE_FROM_HAND ||
            effect == GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS ||
            effect == GameEffect.UPGRADE_DIE_AND_USE_NOW
    }
}
