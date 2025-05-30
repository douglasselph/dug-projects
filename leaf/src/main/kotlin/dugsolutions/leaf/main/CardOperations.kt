package dugsolutions.leaf.main

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.common.Commons.TEST_CARD_LIST
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.tool.CardRegistry

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
