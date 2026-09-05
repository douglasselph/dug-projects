package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.HumanBaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.baseline.placement.HumanBaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.baseline.reward.HumanBaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.baseline.support.HumanBaselineSupportStrategy
import dugsolutions.leaf.v35.player.decision.baseline.wound.HumanBaselineWoundStrategy

/**
 * Canonical simulation baseline: simple, reasonable human play.
 *
 * This layer is intentionally separate from [MechanicalControl]. The scoring
 * and contextual heuristics are added in the next implementation stages. For
 * this layer-clarification step, each HumanBaseline strategy has its own type
 * and delegates to Mechanical Control so existing behavior remains stable
 * while callers can already select the correct semantic layer.
 */
object HumanBaseline {
    const val NAME: String = "Human Baseline"
    const val STRATEGY_LEVEL: Int = 1

    /**
     * False only while the upcoming priority/context implementation is being
     * built. Keeping this explicit prevents simulations from accidentally
     * treating the temporary delegates as the finished human model.
     */
    const val HEURISTICS_IMPLEMENTED: Boolean = false

    fun createDirector(): DecisionDirector =
        DecisionDirector(
            reward = HumanBaselineRewardStrategy(),
            wound = HumanBaselineWoundStrategy(),
            placement = HumanBaselineCreaturePlacementStrategy(),
            cultivation = HumanBaselineCultivationStrategy(),
            battle = HumanBaselineBattleStrategy(),
            buy = HumanBaselineBuyStrategy(),
            support = HumanBaselineSupportStrategy(),
            effect = HumanBaselineEffectStrategy()
        )
}
