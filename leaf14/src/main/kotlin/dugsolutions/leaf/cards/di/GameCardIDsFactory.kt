package dugsolutions.leaf.cards.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.list.GameCardIDs
import dugsolutions.leaf.random.Randomizer

class GameCardIDsFactory(
    private val cardManager: CardManager,
    private val randomizer: Randomizer
) {
    operator fun invoke(initialCardIds: List<CardID>): GameCardIDs {
        return GameCardIDs(cardManager, initialCardIds, randomizer)
    }
}
