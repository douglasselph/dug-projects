package dugsolutions.leaf.cards

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.di.GameCardsFactory

class GetCards(
    private val cardManager: CardManager,
    private val gameCardsFactory: GameCardsFactory
) {

    operator fun invoke(type: FlourishType): GameCards {
        return gameCardsFactory(cardManager.getCardsByType(type)).sortByCost()
    }

}
