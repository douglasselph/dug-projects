package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.decision.wound.BaselineWoundStrategy
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy

class ScriptedWoundStrategy(
    private val fallback: WoundStrategy = BaselineWoundStrategy()
) : WoundStrategy {
    private val choices =
        DecisionScript<ChooseWoundRequest, WoundChoice>("Wound choices")

    fun thenChoose(
        selector: (ChooseWoundRequest) -> WoundChoice
    ): ScriptedWoundStrategy = apply { choices.then(selector) }

    override fun choose(request: ChooseWoundRequest): WoundChoice {
        val chosen = choices.nextOrElse(request, fallback::choose)
        require(chosen in request.legalChoices) {
            "Scripted Wound choice is not legal: $chosen; legal=${request.legalChoices}"
        }
        return chosen
    }

    fun assertExhausted() = choices.assertExhausted()
}
