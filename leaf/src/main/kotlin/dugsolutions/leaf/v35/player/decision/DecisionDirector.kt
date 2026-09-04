package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.placement.BaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.BaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.decision.buy.BaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.battle.BaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.battle.BattleStrategy
import dugsolutions.leaf.v35.player.decision.reward.BaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.player.decision.wound.BaselineWoundStrategy
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy
import dugsolutions.leaf.v35.player.decision.support.BaselineSupportStrategy
import dugsolutions.leaf.v35.player.decision.support.SupportStrategy
import dugsolutions.leaf.v35.player.decision.effect.BaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy

/**
 * Per-player composition of decision policies.
 *
 * This is intentionally a data class so simulations can vary one strategic
 * dimension while leaving all others unchanged:
 *
 * baseline.copy(reward = ExperimentalRewardStrategy())
 *
 * Additional strategy slots should be added only when their v35 coordinators
 * or executors are implemented.
 */
data class DecisionDirector(
    val reward: RewardStrategy,
    val wound: WoundStrategy,
    val placement: CreaturePlacementStrategy,
    val cultivation: CultivationStrategy,
    val battle: BattleStrategy,
    val buy: BuyStrategy,
    val support: SupportStrategy,
    val effect: EffectStrategy
) {
    companion object {

        fun baseline(): DecisionDirector =
            DecisionDirector(
                reward = BaselineRewardStrategy(),
                wound = BaselineWoundStrategy(),
                placement = BaselineCreaturePlacementStrategy(),
                cultivation = BaselineCultivationStrategy(),
                battle = BaselineBattleStrategy(),
                buy = BaselineBuyStrategy(),
                support = BaselineSupportStrategy(),
                effect = BaselineEffectStrategy()
            )
    }
}
