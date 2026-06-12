package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice

sealed interface ExecuteTarget {
    data class PlayerDie(
        val player: Player,
        val dice: Dice
    ) : ExecuteTarget
}
