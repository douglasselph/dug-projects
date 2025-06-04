package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.di.factory.GameCardIDsFactory

class FloralArray(
    private val cardManager: CardManager,
    private val floralCount: FloralCount,
    gameCardIDsFactory: GameCardIDsFactory
) {

    private val stack: GameCardIDs = gameCardIDsFactory(emptyList())

    val cards: List<GameCard>
        get() {
            return stack.cardIds.mapNotNull { id -> cardManager.getCard(id) }
        }

    fun add(cardId: CardID) {
        stack.add(cardId)
    }

    fun remove(cardId: CardID): Boolean {
        return stack.remove(cardId)
    }

    fun floralCount(flowerCardID: CardID): Int {
        return floralCount(stack.cardIds, flowerCardID)
    }

    fun clear() {
        stack.clear()
    }

}
