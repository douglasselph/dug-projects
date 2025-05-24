package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.game.acquire.evaluator.AcquireCardEvaluator
import dugsolutions.leaf.game.acquire.evaluator.AcquireDieEvaluator

interface DecisionAcquireSelect {

    sealed class BuyItem {

        data class Card(val item: AcquireCardEvaluator.BestChoice) : BuyItem()
        data class Die(val item: AcquireDieEvaluator.BestChoice) : BuyItem()
        data object None : BuyItem()
    }

    operator fun invoke(
        bestCard: AcquireCardEvaluator.BestChoice?,
        bestDie: AcquireDieEvaluator.BestChoice?
    ): BuyItem

}
