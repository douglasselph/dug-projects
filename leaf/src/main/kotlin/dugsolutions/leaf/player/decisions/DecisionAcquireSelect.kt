package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.game.purchase.evaluator.PurchaseCardEvaluator
import dugsolutions.leaf.game.purchase.evaluator.PurchaseDieEvaluator
import dugsolutions.leaf.player.Player

interface DecisionAcquireSelect {

    sealed class BuyItem {

        data class Card(val item: PurchaseCardEvaluator.BestChoice) : BuyItem()
        data class Die(val item: PurchaseDieEvaluator.BestChoice) : BuyItem()
        data object None : BuyItem()
    }

    operator fun invoke(
        bestCard: PurchaseCardEvaluator.BestChoice?,
        bestDie: PurchaseDieEvaluator.BestChoice?
    ): BuyItem

}
