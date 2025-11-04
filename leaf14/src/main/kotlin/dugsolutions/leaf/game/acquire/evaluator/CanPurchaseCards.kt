package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.acquire.domain.Combination

class CanPurchaseCards(
    private val canPurchaseCard: CanPurchaseCard
) {

    operator fun invoke(
        marketCards: List<GameCard>,
        combination: Combination
    ): List<GameCard> {
        return marketCards.filter { card -> canPurchaseCard(card, combination) }
    }

}
