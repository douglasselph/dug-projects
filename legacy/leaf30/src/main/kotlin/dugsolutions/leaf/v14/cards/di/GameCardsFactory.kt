package dugsolutions.leaf.v14.cards.di

import dugsolutions.leaf.v14.cards.GameCards
import dugsolutions.leaf.v14.cards.cost.CostScore
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.random.Randomizer

class GameCardsFactory(
    private val randomizer: Randomizer,
    private val costScore: CostScore
) {

    operator fun invoke(cards: List<GameCard>): GameCards {
        return GameCards(cards, randomizer, costScore)
    }

}
