package dugsolutions.leaf.v14.grove.domain

import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.random.die.DieSides

data class MarketConfig(
    val stacks: List<MarketStackConfig>,
    val dice: List<MarketDiceConfig>
)

data class MarketStackConfig(
    val which: MarketStackID,
    val cards: List<MarketCardConfig>? = null
)

data class MarketCardConfig(
    val card: GameCard,
    val count: Int
)

data class MarketDiceConfig(
    val sides: DieSides,
    val count: Int
)
