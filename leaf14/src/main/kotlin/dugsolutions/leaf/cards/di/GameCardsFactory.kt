package dugsolutions.leaf.cards.di

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.list.GameCards
import dugsolutions.leaf.random.Randomizer

class GameCardsFactory(
    private val randomizer: Randomizer
) {

    operator fun invoke(cards: List<GameCard>): GameCards {
        return GameCards(cards, randomizer)
    }

}
