package dugsolutions.leaf.game.actions.choices

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.player.Player

class AcquireCard(
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, action: Action.AcquireCard) {
        if (player.discard(action.using)) {
            player.addCardToDiscard(action.card.id)
            chronicle(Moment.ACQUIRE_CARD(player, action))
        } else {
            chronicle(Moment.ACQUIRE_ERROR(player, action, "Acquire Card"))
        }
    }

}
