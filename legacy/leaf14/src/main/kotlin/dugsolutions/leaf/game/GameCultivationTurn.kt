package dugsolutions.leaf.game

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.actions.ExecuteActions
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.player.Player

class GameCultivationTurn(
    private val playerOrder: PlayerOrder,
    private val executeActions: ExecuteActions,
    private val handleCleanup: HandleCleanup,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(players: List<Player>) {
        reportHand(players)
        val orderedPlayers = playerOrder(players)
        orderedPlayers.forEach { player -> executeActions(player) }
        orderedPlayers.forEach { player -> handleCleanup(player) }
    }

    private fun reportHand(players: List<Player>) {
        players.forEach { chronicle(Moment.REPORT_HAND(it)) }
    }
}
