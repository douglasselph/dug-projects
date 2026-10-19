package dugsolutions.leaf.v14.player.components

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.domain.CardID
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.cards.GameCardIDs
import dugsolutions.leaf.v14.cards.di.GameCardIDsFactory

class FloralArray(
    private val cardManager: CardManager,
    gameCardIDsFactory: GameCardIDsFactory
) {

    private val stack: GameCardIDs = gameCardIDsFactory(emptyList())

    val cards: List<GameCard>
        get() {
            return stack.cardIds.mapNotNull { id -> cardManager.getCard(id) }
        }

    val cardIds: List<CardID>
        get() = stack.cardIds

    fun add(cardId: CardID) {
        stack.add(cardId)
    }

    fun remove(cardId: CardID): Boolean {
        return stack.remove(cardId)
    }

    fun clear() {
        stack.clear()
    }

}
