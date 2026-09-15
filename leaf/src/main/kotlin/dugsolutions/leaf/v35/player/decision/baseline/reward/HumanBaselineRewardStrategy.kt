package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.reward.MechanicalRewardStrategy
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.tokens.Critter

class HumanBaselineRewardStrategy(
    private val delegate: RewardStrategy = MechanicalRewardStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine()
) : RewardStrategy {
    override fun chooseCritter(request: ChooseCritterRequest): Critter {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCritter(request)
        return scoreEngine.chooseValue(
            request.legalChoices.map { choice ->
                ScoredChoice(choice, CritterRewardPriority.score(request.context, choice))
            }
        )
    }
}
