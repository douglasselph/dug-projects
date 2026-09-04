package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.decision.cultivation.BaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy

class ScriptedCultivationStrategy(
    private val fallback: CultivationStrategy = BaselineCultivationStrategy()
) : CultivationStrategy {
    private val actions =
        DecisionScript<ChooseCultivationActionRequest, CultivationAction>(
            "Cultivation actions"
        )

    fun thenChoose(
        selector: (ChooseCultivationActionRequest) -> CultivationAction
    ): ScriptedCultivationStrategy = apply {
        actions.then(selector)
    }

    fun thenChoose(choice: CultivationAction): ScriptedCultivationStrategy =
        thenChoose { choice }

    override fun chooseAction(
        request: ChooseCultivationActionRequest
    ): CultivationAction {
        val chosen = actions.nextOrElse(request, fallback::chooseAction)
        require(chosen in request.legalChoices) {
            "Scripted Cultivation choice is not legal: $chosen; " +
                "legal=${request.legalChoices}"
        }
        return chosen
    }

    fun assertExhausted() = actions.assertExhausted()
}
