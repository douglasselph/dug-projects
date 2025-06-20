package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player

class HandleCleanup(
    private val handleReused: HandleReused,
    private val handleRetained: HandleRetained,
    private val handleCompostRecovery: HandleCompostRecovery,
) {

    suspend operator fun invoke(player: Player) {
        player.discardHand()
        player.drawHand()
        handleReused(player)
        handleRetained(player)
        handleCompostRecovery(player)
    }

}
