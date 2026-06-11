package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Die

sealed interface ExecuteTarget {
    data class PlayerDie(
        val player: Player,
        val die: Die
    ) : ExecuteTarget
}
