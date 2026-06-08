package dugsolutions.leaf.v14.main.local

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.GameCards
import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.cards.di.GameCardsFactory
import dugsolutions.leaf.v14.main.domain.CardInfo
import dugsolutions.leaf.v14.cards.CardRegistry
import dugsolutions.leaf.v14.common.Commons

class CardOperations(
    private val cardManager: CardManager,
    private val cardRegistry: CardRegistry,
    private val gameCardsFactory: GameCardsFactory
) {

    fun setup() {
        cardRegistry.loadFromCsv(Commons.CARD_LIST)
        cardManager.loadCards(cardRegistry)
    }

    fun getGameCards(type: FlourishType): GameCards {
        return gameCardsFactory(cardManager.getCardsByType(type)).sortByCost()
    }

    fun getCard(cardInfo: CardInfo): GameCard? {
        return cardManager.getCard(cardInfo.name)
    }

}
