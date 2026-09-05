package dugsolutions.leaf.v35.player.decision.baseline.buy

import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy

/**
 * Human Baseline buy policy shell.
 *
 * The Human Baseline scoring/context implementation will replace this
 * delegate incrementally. Keeping a distinct type now cleanly separates the
 * simulation baseline from Mechanical Control without changing behavior yet.
 */
class HumanBaselineBuyStrategy(
    private val delegate: BuyStrategy = MechanicalBuyStrategy()
) : BuyStrategy by delegate
