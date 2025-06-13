package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player

class HandleCleanup(
    private val handleReused: HandleReused,
    private val handleRetained: HandleRetained,
    private val handleCompostRecovery: HandleCompostRecovery,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(player: Player) {
        player.discardHand()
        player.drawHand()
        handleReused(player)
        handleRetained(player)
        handleCompostRecovery(player) // TODO: Unit test
    }

}
