package dugsolutions.leaf.v30.player.decision.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.round.domain.RoundCard

sealed interface Decision {

    data class ChooseCritter(
        val player: Player,
        val availableCritters: List<Critter>
    ) : Decision

    data class ChooseMainAction(
        val player: Player,
        val roundCard: RoundCard,
        val actionsRemaining: Int
    ) : Decision

    data class ChooseItemsToBuy(
        val player: Player,
        val grove: Grove
    ) : Decision
}
