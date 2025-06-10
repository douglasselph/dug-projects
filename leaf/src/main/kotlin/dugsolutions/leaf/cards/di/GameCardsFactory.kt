package dugsolutions.leaf.cards.di

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.Randomizer

class GameCardsFactory(
    private val randomizer: Randomizer,
    private val costScore: CostScore
) {

    operator fun invoke(cards: List<GameCard>): GameCards {
        return GameCards(cards, randomizer, costScore)
    }

}
