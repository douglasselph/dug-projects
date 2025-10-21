package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.acquire.UsingDice

class CanPurchaseCards(
    private val canPurchaseCard: CanPurchaseCard
) {

    operator fun invoke(
        marketCards: List<GameCard>,
        usingDice: UsingDice
    ): List<GameCard> {
        return marketCards.filter { card -> canPurchaseCard(card,  usingDice) }
    }

}
