package dugsolutions.leaf.v35.player.decision.baseline.placement

import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.placement.MechanicalCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest
import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy

class HumanBaselineCreaturePlacementStrategy(
    private val delegate: CreaturePlacementStrategy = MechanicalCreaturePlacementStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry()
) : CreaturePlacementStrategy {
    override fun choose(request: ChooseCreaturePlacementRequest): GraftPlacement {
        if (request.context == DecisionContext.EMPTY) return delegate.choose(request)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalPlacements.map { placement ->
                DecisionCandidate(
                    choice = placement,
                    score = GraftPlacementPriority.score(request.context, request.card.type, placement)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }
}
