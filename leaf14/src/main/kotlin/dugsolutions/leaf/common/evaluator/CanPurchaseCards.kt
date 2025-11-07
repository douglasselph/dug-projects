package dugsolutions.leaf.common.evaluator

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.DieValues

class CanPurchaseCards(
    private val canPurchaseCard: CanPurchaseCard
) {

    operator fun invoke(
        marketCards: List<GameCard>,
        values: DieValues
    ): List<GameCard> {
        return marketCards.filter { card -> canPurchaseCard(card, values) }
    }

}
