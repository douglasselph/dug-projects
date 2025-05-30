package dugsolutions.leaf.grove.scenario

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.GetCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.grove.domain.MarketCardConfig
import dugsolutions.leaf.grove.domain.MarketStackConfig
import dugsolutions.leaf.grove.domain.MarketStackID

open class ScenarioBase(
    private val getCards: GetCards,
) {

    protected fun getCards(type: FlourishType): GameCards {
        return getCards(type)
    }

    protected fun getMarketStackConfig(
        which: MarketStackID,
        cards: List<GameCard>
    ): MarketStackConfig {
        val stack = mutableListOf<MarketCardConfig>()
        for (card in cards) {
            stack.add(MarketCardConfig(card, 8))
        }
        return MarketStackConfig(which, stack)
    }

}
