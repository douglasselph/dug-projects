package dugsolutions.leaf.di.factory

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.tool.Randomizer

class GameCardsFactory(
    private val randomizer: Randomizer,
    private val costScore: CostScore
) {

    operator fun invoke(cards: List<GameCard>): GameCards {
        return GameCards(cards, randomizer, costScore)
    }

}
