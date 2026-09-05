package dugsolutions.leaf.v35.player.decision.baseline.support

import dugsolutions.leaf.v35.player.decision.support.SupportStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.support.MechanicalSupportStrategy

/**
 * Human Baseline support policy shell.
 *
 * The Human Baseline scoring/context implementation will replace this
 * delegate incrementally. Keeping a distinct type now cleanly separates the
 * simulation baseline from Mechanical Control without changing behavior yet.
 */
class HumanBaselineSupportStrategy(
    private val delegate: SupportStrategy = MechanicalSupportStrategy()
) : SupportStrategy by delegate
