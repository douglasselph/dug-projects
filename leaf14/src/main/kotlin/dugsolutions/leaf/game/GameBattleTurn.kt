package dugsolutions.leaf.game

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.actions.ExecuteActions
import dugsolutions.leaf.game.battle.HandleInsects
import dugsolutions.leaf.game.battle.ResolveBattle
import dugsolutions.leaf.game.battle.domain.PlayerValues
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.player.Player

class GameBattleTurn(
    private val playerOrder: PlayerOrder,
    private val executeActions: ExecuteActions,
    private val handleInsects: HandleInsects,
    private val resolveBattle: ResolveBattle,
    private val handleCleanup: HandleCleanup,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(players: List<Player>) {
        reportHand(players)
        val orderedPlayers = playerOrder(players)
        orderedPlayers.forEach { player -> executeActions(player) }
        val battleGrid = mutableListOf<PlayerValues>()
        orderedPlayers.forEach { player -> battleGrid.add(handleInsects(player)) }
        resolveBattle(battleGrid)
        orderedPlayers.forEach { player -> handleCleanup(player) }
    }

    private fun reportHand(players: List<Player>) {
        players.forEach { chronicle(Moment.REPORT_HAND(it)) }
    }
}
