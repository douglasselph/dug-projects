package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.CardType
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.grove.domain.GroveCardStackID

class GainGameCard(
    private val grove: Grove
) {
    operator fun invoke(type: CardType): GameCard? {
        val stackId = GroveCardStackID.entries
            .filter { it.type == type }
            .sortedByDescending { it.cost }
            .firstOrNull { grove.has(it) }
            ?: return null
        val card = grove.getCard(stackId) ?: return null
        if (!grove.remove(stackId)) return null
        return card
    }
}
