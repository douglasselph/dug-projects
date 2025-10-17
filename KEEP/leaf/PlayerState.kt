package dugsolutions.leaf.player.components

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.GameCard

/**
 * Information to be remembered across turns.
 */
class PlayerState {
    var isDormant: Boolean = false
    var bonusDie: Die? = null
    var hasPassed = false
    var wasHit = false
    val didNotTrash = mutableListOf<GameCard>()

    fun clear() {
        isDormant = false
        bonusDie = null
        hasPassed = false
        wasHit = false
        didNotTrash.clear()
    }

}