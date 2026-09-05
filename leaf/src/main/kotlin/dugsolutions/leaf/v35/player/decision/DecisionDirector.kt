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
 * Core exposes two named complete directors:
 * - [mechanicalControl]: deterministic engine/test control behavior.
 * - [humanBaseline]: canonical ordinary-human simulation baseline.
 *
 * The data-class shape remains important because simulation code can replace
 * one decision area while leaving all other areas unchanged.
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

        fun mechanicalControl(): DecisionDirector =
            MechanicalControl.createDirector()

        fun humanBaseline(): DecisionDirector =
            HumanBaseline.createDirector()

        /**
         * Canonical baseline now means Human Baseline. This name is retained
         * for source compatibility while callers migrate to [humanBaseline].
         */
        fun baseline(): DecisionDirector =
            humanBaseline()

        /** Backward-compatible old name for Mechanical Control. */
        @Deprecated(
            message = "Use mechanicalControl()",
            replaceWith = ReplaceWith("mechanicalControl()")
        )
        fun mechanicalBaseline(): DecisionDirector =
            mechanicalControl()
    }
}
