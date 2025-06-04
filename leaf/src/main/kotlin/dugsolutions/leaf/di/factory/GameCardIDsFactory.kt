package dugsolutions.leaf.di.factory

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.GameCardIDs
import dugsolutions.leaf.tool.Randomizer

class GameCardIDsFactory(
    private val cardManager: CardManager,
    private val randomizer: Randomizer
) {
    operator fun invoke(initialCardIds: List<CardID>): GameCardIDs {
        return GameCardIDs(cardManager, initialCardIds, randomizer)
    }
}
