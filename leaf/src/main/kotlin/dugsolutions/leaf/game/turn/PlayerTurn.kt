package dugsolutions.leaf.game.turn

import dugsolutions.leaf.game.battle.HandleDeliverDamage
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.acquire.HandleGroveAcquisition
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.game.turn.handle.HandleGetTarget
import dugsolutions.leaf.player.Player

class PlayerTurn(
    private val playerRound: PlayerRound,
    private val playerOrder: PlayerOrder,
    private val handleDeliverDamage: HandleDeliverDamage,
    private val handleGetTarget: HandleGetTarget,
    private val handleGroveAcquisition: HandleGroveAcquisition,
    private val handleCleanup: HandleCleanup
) {

    operator fun invoke(players: List<Player>, phase: GamePhase) {
        var orderedPlayers = playerOrder(players)
        orderedPlayers.forEach { player ->
            val target = handleGetTarget(player, orderedPlayers)
            playerRound(player, target)
        }
        orderedPlayers = playerOrder(players)
        if (phase == GamePhase.BATTLE) {
            handleDeliverDamage(orderedPlayers)
        } else {
            orderedPlayers.forEach { player -> handleGroveAcquisition(player) }
        }
        orderedPlayers.forEach { player -> handleCleanup(player) }
    }

}
