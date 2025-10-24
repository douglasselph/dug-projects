package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard

class WispManager {

    private val wisps = mutableListOf<GameCard>()

    fun add(wisp: GameCard) {
        if (wisp.type != FlourishType.WISP) {
            throw IllegalArgumentException("Only cards with FlourishType.WISP can be added to WispManager. Received: ${wisp.type}")
        }
        wisps.add(wisp)
    }
    
    fun remove(wisp: GameCard) = wisps.remove(wisp)
    fun has(wisp: GameCard) = wisps.contains(wisp)

}
