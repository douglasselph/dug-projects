package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.decision.cultivation.BaselineCultivationStrategy
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.decision.support.SupportAction

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


    fun thenMain(action: CultivationMainAction): ScriptedCultivationStrategy =
        thenChoose(CultivationAction.Main(action))

    fun thenSupport(
        selector: (SupportAction) -> Boolean
    ): ScriptedCultivationStrategy = thenChoose { request ->
        request.legalChoices
            .filterIsInstance<CultivationAction.Support>()
            .first { selector(it.action) }
    }

    fun thenDone(): ScriptedCultivationStrategy =
        thenChoose(CultivationAction.Done)

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
