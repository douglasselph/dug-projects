package dugsolutions.leaf.grove.domain

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.DieSides

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
