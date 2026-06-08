package dugsolutions.leaf.v14.grove.scenario

import dugsolutions.leaf.v14.cards.GameCards
import dugsolutions.leaf.v14.cards.domain.FlourishType
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.grove.domain.MarketCardConfig
import dugsolutions.leaf.v14.grove.domain.MarketStackConfig
import dugsolutions.leaf.v14.grove.domain.MarketStackID
import dugsolutions.leaf.v14.main.local.CardOperations

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
