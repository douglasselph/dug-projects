package dugsolutions.leaf.v35.player.decision.baseline.placement

import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.placement.MechanicalCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest
import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy

class HumanBaselineCreaturePlacementStrategy(
    private val delegate: CreaturePlacementStrategy = MechanicalCreaturePlacementStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine()
) : CreaturePlacementStrategy {
    override fun choose(request: ChooseCreaturePlacementRequest): GraftPlacement {
        if (request.context == DecisionContext.EMPTY) return delegate.choose(request)
        return scoreEngine.chooseValue(
            request.legalPlacements.map { placement ->
                ScoredChoice(
                    placement,
                    GraftPlacementPriority.score(request.context, request.card.type, placement)
                )
            }
        )
    }
}
