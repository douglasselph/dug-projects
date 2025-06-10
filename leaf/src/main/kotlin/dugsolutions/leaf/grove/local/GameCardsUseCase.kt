package dugsolutions.leaf.grove.local

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.grove.domain.MarketCardConfig

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
