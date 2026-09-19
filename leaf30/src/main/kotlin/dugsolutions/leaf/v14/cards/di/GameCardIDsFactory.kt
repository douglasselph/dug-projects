package dugsolutions.leaf.v14.cards.di

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.domain.CardID
import dugsolutions.leaf.v14.cards.GameCardIDs
import dugsolutions.leaf.v14.random.Randomizer

class GameCardIDsFactory(
    private val cardManager: CardManager,
    private val randomizer: Randomizer
) {
    operator fun invoke(initialCardIds: List<CardID>): GameCardIDs {
        return GameCardIDs(cardManager, initialCardIds, randomizer)
    }
}
