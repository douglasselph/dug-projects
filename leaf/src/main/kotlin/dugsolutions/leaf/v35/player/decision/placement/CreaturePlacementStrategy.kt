package dugsolutions.leaf.v35.player.decision.placement

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.creature.GraftPlacement

/**
 * Immutable information needed to choose where a newly gained Plant card
 * should be grafted.
 *
 * Creature owns legality. Gameplay code should obtain legal placements from
 * Creature and supply them here. The strategy chooses one placement but does
 * not graft the card itself.
 */
class ChooseCreaturePlacementRequest(
    val card: PlantCard,
    legalPlacements: List<GraftPlacement>,
    val context: DecisionContext = DecisionContext.EMPTY
) {
    val legalPlacements: List<GraftPlacement> =
        legalPlacements.toList()

    init {
        require(this.legalPlacements.isNotEmpty()) {
            "Creature placement decision requires at least one legal placement"
        }
    }
}

interface CreaturePlacementStrategy {

    fun choose(
        request: ChooseCreaturePlacementRequest
    ): GraftPlacement
}
