package dugsolutions.leaf.grove.domain

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.di.GameCardsFactory

class GameCardsUseCase(
    private val gameCardsFactory: GameCardsFactory
) {

    operator fun invoke(configs: List<MarketCardConfig>): GameCards {
        val result = mutableListOf<GameCard>()
        for (config in configs) {
            repeat(config.count) { result.add(config.card) }
        }
        return gameCardsFactory(result)
    }

}
