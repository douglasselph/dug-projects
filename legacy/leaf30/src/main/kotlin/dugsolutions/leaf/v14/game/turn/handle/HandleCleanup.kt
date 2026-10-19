package dugsolutions.leaf.v14.game.turn.handle

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.player.Player

class HandleCleanup(
    private val handleReused: HandleReused,
    private val handleRetained: HandleRetained,
    private val handleCompostRecovery: HandleCompostRecovery,
    private val handleDrawHand: HandleDrawHand,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player) {
        player.discardHand()
        handleDrawHand(player)
        val numReused = handleReused(player)
        val numRetained = handleRetained(player)
        handleCompostRecovery(player)
        chronicle(Moment.CLEANUP(player, numReused, numRetained))
    }

}
