package dugsolutions.leaf.v14.grove.local

import dugsolutions.leaf.v14.cards.GameCards
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.cards.di.GameCardsFactory
import dugsolutions.leaf.v14.grove.domain.MarketCardConfig

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
