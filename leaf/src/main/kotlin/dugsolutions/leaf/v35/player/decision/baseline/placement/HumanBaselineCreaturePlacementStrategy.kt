package dugsolutions.leaf.v35.player.decision.baseline.placement

import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.placement.MechanicalCreaturePlacementStrategy

/**
 * Human Baseline placement policy shell.
 *
 * The Human Baseline scoring/context implementation will replace this
 * delegate incrementally. Keeping a distinct type now cleanly separates the
 * simulation baseline from Mechanical Control without changing behavior yet.
 */
class HumanBaselineCreaturePlacementStrategy(
    private val delegate: CreaturePlacementStrategy = MechanicalCreaturePlacementStrategy()
) : CreaturePlacementStrategy by delegate
