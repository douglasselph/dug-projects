package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.reward.MechanicalRewardStrategy

/**
 * Human Baseline reward policy shell.
 *
 * The Human Baseline scoring/context implementation will replace this
 * delegate incrementally. Keeping a distinct type now cleanly separates the
 * simulation baseline from Mechanical Control without changing behavior yet.
 */
class HumanBaselineRewardStrategy(
    private val delegate: RewardStrategy = MechanicalRewardStrategy()
) : RewardStrategy by delegate
