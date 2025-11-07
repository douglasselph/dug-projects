package dugsolutions.leaf.common.evaluator

import dugsolutions.leaf.cards.domain.Cost
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.DieValues

class CanPurchaseCard {

    operator fun invoke(
        card: GameCard,
        values: DieValues
    ): Boolean {
        // If no cost elements, the card is free
        val value = card.cost as? Cost.Value ?: return true
        return canBuy(value, values)
    }

    private fun canBuy(
        value: Cost.Value,
        values: DieValues
    ): Boolean {
        return (values.total >= value.amount)
    }

}
