package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.Player

sealed interface Decision {

    data class ChooseCritter(
        val player: Player,
        val availableCritters: List<Critter>
    ) : Decision
}
