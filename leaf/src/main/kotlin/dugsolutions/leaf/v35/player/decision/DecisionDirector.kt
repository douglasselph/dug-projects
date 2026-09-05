package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.battle.BattleStrategy
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.player.decision.support.SupportStrategy
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy

/**
 * Per-player composition of decision policies.
 *
 * This is intentionally a data class so simulations can vary one strategic
 * dimension while leaving all others unchanged:
 *
 * DecisionDirector.mechanicalBaseline().copy(reward = ExperimentalRewardStrategy())
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

        /** Preferred explicit name for the stable Strategy Level-0 control. */
        fun mechanicalBaseline(): DecisionDirector =
            MechanicalBaseline.createDirector()

        /**
         * Backward-compatible shorthand. New simulation code should prefer
         * [mechanicalBaseline] so the control strategy is named explicitly.
         */
        fun baseline(): DecisionDirector =
            mechanicalBaseline()
    }
}
