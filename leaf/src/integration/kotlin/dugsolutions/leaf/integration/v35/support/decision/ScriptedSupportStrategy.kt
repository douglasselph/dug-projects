package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.decision.support.BaselineSupportStrategy
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.SupportStrategy

class ScriptedSupportStrategy(
    private val fallback: SupportStrategy = BaselineSupportStrategy()
) : SupportStrategy {
    private val butterflyRolls =
        DecisionScript<ChooseButterflyRollRequest, ButterflyRollChoice>(
            "Butterfly roll choices"
        )

    fun thenButterflyRoll(
        selector: (ChooseButterflyRollRequest) -> ButterflyRollChoice
    ): ScriptedSupportStrategy = apply { butterflyRolls.then(selector) }

    override fun chooseButterflyRoll(
        request: ChooseButterflyRollRequest
    ): ButterflyRollChoice =
        butterflyRolls.nextOrElse(request, fallback::chooseButterflyRoll)

    fun assertExhausted() = butterflyRolls.assertExhausted()
}
