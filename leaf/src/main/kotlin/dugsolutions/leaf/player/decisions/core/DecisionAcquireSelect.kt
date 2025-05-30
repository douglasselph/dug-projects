package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.game.acquire.evaluator.AcquireCardEvaluator
import dugsolutions.leaf.game.acquire.evaluator.AcquireDieEvaluator

interface DecisionAcquireSelect {

    sealed class BuyItem {

        data class Card(val item: AcquireCardEvaluator.Choice) : BuyItem()
        data class Die(val item: AcquireDieEvaluator.BestChoice) : BuyItem()
        data object None : BuyItem()
    }

    operator fun invoke(
        bestCard: AcquireCardEvaluator.Choice?,
        bestDie: AcquireDieEvaluator.BestChoice?
    ): BuyItem

}
