package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.domain.Cost
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.acquire.UsingDice

class CanPurchaseCard {

    operator fun invoke(
        card: GameCard,
        usingDice: UsingDice,
    ): Boolean {
        // If no cost elements, the card is free
        val value = card.cost as? Cost.Value ?: return true
        return canBuy(value, usingDice)
    }

    private fun canBuy(
        value: Cost.Value,
        usingDice: UsingDice,
    ): Boolean {
        return ((usingDice.totalValue + usingDice.addToTotal) >= value.amount)
    }

}
