package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.mechanical.placement.MechanicalCreaturePlacementStrategy
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest
import dugsolutions.leaf.v35.player.decision.placement.CreaturePlacementStrategy

class ScriptedCreaturePlacementStrategy(
    private val fallback: CreaturePlacementStrategy = MechanicalCreaturePlacementStrategy()
) : CreaturePlacementStrategy {
    private val choices =
        DecisionScript<ChooseCreaturePlacementRequest, GraftPlacement>(
            "Creature placements"
        )

    fun thenChoose(
        selector: (ChooseCreaturePlacementRequest) -> GraftPlacement
    ): ScriptedCreaturePlacementStrategy = apply { choices.then(selector) }

    override fun choose(request: ChooseCreaturePlacementRequest): GraftPlacement {
        val chosen = choices.nextOrElse(request, fallback::choose)
        require(chosen in request.legalPlacements) {
            "Scripted graft placement is not legal: $chosen; " +
                "legal=${request.legalPlacements}"
        }
        return chosen
    }

    fun assertExhausted() = choices.assertExhausted()
}
