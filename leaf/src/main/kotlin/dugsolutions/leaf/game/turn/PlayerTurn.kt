package dugsolutions.leaf.game.turn

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
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
    private val handleCleanup: HandleCleanup,
    private val chronicle: GameChronicle
) {

    suspend operator fun invoke(players: List<Player>, phase: GamePhase) {
        reportHand(players)
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

    private fun reportHand(players: List<Player>) {
        players.forEach { chronicle(Moment.DRAWN_HAND(it)) }
    }

}
