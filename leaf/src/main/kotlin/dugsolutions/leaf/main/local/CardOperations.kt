package dugsolutions.leaf.main.local

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.common.Commons.TEST_CARD_LIST
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.cards.CardRegistry

class CardOperations(
    private val cardManager: CardManager,
    private val cardRegistry: CardRegistry,
    private val gameCardsFactory: GameCardsFactory
) {

    fun setup() {
        cardRegistry.loadFromCsv(TEST_CARD_LIST)
        cardManager.loadCards(cardRegistry)
    }

    fun getGameCards(type: FlourishType): GameCards {
        return gameCardsFactory(cardManager.getCardsByType(type)).sortByCost()
    }

    fun getCard(cardInfo: CardInfo): GameCard? {
        return cardManager.getCard(cardInfo.name)
    }

}
