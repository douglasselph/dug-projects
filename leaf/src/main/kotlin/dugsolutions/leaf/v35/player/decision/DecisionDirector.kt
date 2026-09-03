package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.placement.BaselineCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.reward.BaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.player.decision.wound.BaselineWoundStrategy
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy

/**
 * Per-player composition of decision policies.
 *
 * This is intentionally a data class so simulations can vary one strategic
 * dimension while leaving all others unchanged:
 *
 * baseline.copy(reward = ExperimentalRewardStrategy())
 *
 * Cultivation, Battle, Buy, and effect-specific strategy slots should be added
 * only when those v35 coordinators are implemented.
 */
data class DecisionDirector(
    val reward: RewardStrategy,
    val wound: WoundStrategy,
    val placement: CreaturePlacementStrategy
) {
    companion object {

        fun baseline(): DecisionDirector =
            DecisionDirector(
                reward = BaselineRewardStrategy(),
                wound = BaselineWoundStrategy(),
                placement = BaselineCreaturePlacementStrategy()
            )
    }
}
