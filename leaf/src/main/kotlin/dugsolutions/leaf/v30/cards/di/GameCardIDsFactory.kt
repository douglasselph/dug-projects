package dugsolutions.leaf.v30.cards.di

import dugsolutions.leaf.v30.cards.GameCardIDs
import dugsolutions.leaf.v30.cards.GameCardManager
import dugsolutions.leaf.v30.cards.domain.GameCardID
import dugsolutions.leaf.v30.random.Randomizer


class GameCardIDsFactory(
    private val cardManager: GameCardManager,
    private val randomizer: Randomizer
) {
    operator fun invoke(initialCardIds: List<GameCardID>): GameCardIDs {
        return GameCardIDs(cardManager, initialCardIds, randomizer)
    }
}
