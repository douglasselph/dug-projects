package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.wound.MechanicalWoundStrategy
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import dugsolutions.leaf.v35.player.decision.wound.WoundStrategy

class HumanBaselineWoundStrategy(
    private val delegate: WoundStrategy = MechanicalWoundStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine()
) : WoundStrategy {
    override fun choose(request: ChooseWoundRequest): WoundChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choose(request)
        return scoreEngine.chooseValue(
            request.legalChoices.map { ScoredChoice(it, WoundPriority.score(request.context, it)) }
        )
    }
}
