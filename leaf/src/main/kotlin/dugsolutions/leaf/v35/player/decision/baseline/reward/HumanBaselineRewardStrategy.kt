package dugsolutions.leaf.v35.player.decision.baseline.reward

import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.reward.MechanicalRewardStrategy
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.reward.RewardStrategy
import dugsolutions.leaf.v35.tokens.Critter

class HumanBaselineRewardStrategy(
    private val delegate: RewardStrategy = MechanicalRewardStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry()
) : RewardStrategy {
    override fun chooseCritter(request: ChooseCritterRequest): Critter {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCritter(request)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalChoices.map { choice ->
                DecisionCandidate(
                    choice = choice,
                    score = CritterRewardPriority.score(request.context, choice),
                    tags = CritterRewardPriority.tags(choice)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }
}
