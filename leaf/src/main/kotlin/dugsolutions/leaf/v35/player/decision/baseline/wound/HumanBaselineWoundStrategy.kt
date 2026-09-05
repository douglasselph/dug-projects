package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.wound.MechanicalWoundStrategy

/**
 * Human Baseline wound policy shell.
 *
 * The Human Baseline scoring/context implementation will replace this
 * delegate incrementally. Keeping a distinct type now cleanly separates the
 * simulation baseline from Mechanical Control without changing behavior yet.
 */
class HumanBaselineWoundStrategy(
    private val delegate: WoundStrategy = MechanicalWoundStrategy()
) : WoundStrategy by delegate
