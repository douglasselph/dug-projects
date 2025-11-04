package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.domain.Cost
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.acquire.domain.Combination

class CanPurchaseCard {

    operator fun invoke(
        card: GameCard,
        combination: Combination
    ): Boolean {
        // If no cost elements, the card is free
        val value = card.cost as? Cost.Value ?: return true
        return canBuy(value, combination)
    }

    private fun canBuy(
        value: Cost.Value,
        combination: Combination
    ): Boolean {
        return ((combination.totalValue) >= value.amount)
    }

}
