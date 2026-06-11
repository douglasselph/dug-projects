package dugsolutions.leaf.v30.chronicle.decision

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.decision.domain.CardsToRefresh
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.decision.domain.ItemsToBuy
import dugsolutions.leaf.v30.player.decision.domain.MainAction

class DecisionDirectorCounting(
    private val delegate: DecisionDirector,
    private val decisionCountLog: DecisionCountLog,
    private val decisionCostEvaluator: DecisionCostEvaluator = DecisionCostEvaluatorBaseline()
) : DecisionDirector {

    override fun chooseCritter(input: Decision.ChooseCritter): Critter {
        decisionCountLog(decisionCostEvaluator(input))
        return delegate.chooseCritter(input)
    }

    override fun chooseMainAction(input: Decision.ChooseMainAction): MainAction {
        decisionCountLog(decisionCostEvaluator(input))
        return delegate.chooseMainAction(input)
    }

    override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy {
        decisionCountLog(decisionCostEvaluator(input))
        return delegate.chooseItemsToBuy(input)
    }

    override fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh {
        decisionCountLog(decisionCostEvaluator(input))
        return delegate.chooseCardsToRefreshWithWorms(input)
    }
}
