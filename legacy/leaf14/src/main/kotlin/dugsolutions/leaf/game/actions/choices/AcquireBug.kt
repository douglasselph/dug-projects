package dugsolutions.leaf.game.actions.choices

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.player.Player

class AcquireBug(
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, action: Action.AcquireBug) {
        if (player.discard(action.using)) {
            player.addBug(action.bug)
            chronicle(Moment.ACQUIRE_BUG(player, action))
        } else {
            chronicle(Moment.ACQUIRE_ERROR(player, action, "Acquire Bug"))
        }
    }

}
