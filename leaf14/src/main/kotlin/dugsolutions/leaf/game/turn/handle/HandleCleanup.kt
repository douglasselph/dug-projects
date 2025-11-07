package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class HandleCleanup(
    private val handleDrawHand: HandleDrawHand,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player) {
        player.discardHand()
        handleDrawHand(player)
        chronicle(Moment.CLEANUP(player))
    }

}
