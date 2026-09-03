package dugsolutions.leaf.v35.player.decision.placement

import dugsolutions.leaf.v35.player.creature.GraftPlacement

/**
 * Deterministic baseline placement policy: choose the first legal placement.
 *
 * More sophisticated simulation strategies can later evaluate card type,
 * geometry, branch shape, future graft opportunities, and other factors.
 */
class BaselineCreaturePlacementStrategy : CreaturePlacementStrategy {

    override fun choose(
        request: ChooseCreaturePlacementRequest
    ): GraftPlacement =
        request.legalPlacements.first()
}
