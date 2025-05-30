package dugsolutions.leaf.grove.scenario

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.grove.domain.MarketCardConfig
import dugsolutions.leaf.grove.domain.MarketStackConfig
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.CardOperations

open class ScenarioBase(
    private val cardOperations: CardOperations
) {

    protected fun getGameCards(type: FlourishType): GameCards {
        return cardOperations.getGameCards(type)
    }

    protected fun getMarketStackConfig(
        which: MarketStackID,
        cards: List<GameCard>,
        countOfEach: Int
    ): MarketStackConfig {
        val stack = mutableListOf<MarketCardConfig>()
        for (card in cards) {
            stack.add(MarketCardConfig(card, countOfEach))
        }
        return MarketStackConfig(which, stack)
    }

}
