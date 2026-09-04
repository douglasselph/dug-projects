package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.player.decision.reward.BaselineRewardStrategy
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.tokens.Critter

class ScriptedRewardStrategy(
    private val fallback: RewardStrategy = BaselineRewardStrategy()
) : RewardStrategy {
    private val choices =
        DecisionScript<ChooseCritterRequest, Critter>("Critter rewards")

    fun thenChoose(
        selector: (ChooseCritterRequest) -> Critter
    ): ScriptedRewardStrategy = apply { choices.then(selector) }

    override fun chooseCritter(request: ChooseCritterRequest): Critter {
        val chosen = choices.nextOrElse(request, fallback::chooseCritter)
        require(chosen in request.legalChoices) {
            "Scripted Critter reward is not legal: $chosen; legal=${request.legalChoices}"
        }
        return chosen
    }

    fun assertExhausted() = choices.assertExhausted()
}
