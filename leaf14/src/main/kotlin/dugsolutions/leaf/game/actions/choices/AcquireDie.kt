package dugsolutions.leaf.game.actions.choices

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.di.DieFactory

class AcquireDie(
    private val dieFactory: DieFactory,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, action: Action.AcquireDie) {
        if (player.discard(action.using)) {
            player.addDieToDiscard(dieFactory(action.sides))
            chronicle(Moment.ACQUIRE_DIE(player, action))
        } else {
            chronicle(Moment.ACQUIRE_ERROR(player, action, "Acquire Die"))
        }
    }

}
