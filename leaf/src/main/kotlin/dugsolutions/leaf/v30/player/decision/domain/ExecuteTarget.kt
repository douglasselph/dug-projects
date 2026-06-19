package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.random.die.Dice

data class ExecuteTarget(
    val player: Player? = null,
    val dice: Dice = Dice(),
    val critter: List<Critter> = emptyList()
)
