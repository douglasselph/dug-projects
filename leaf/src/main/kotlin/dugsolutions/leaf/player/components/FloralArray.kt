package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.GameCardIDs
import dugsolutions.leaf.cards.di.GameCardIDsFactory

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
