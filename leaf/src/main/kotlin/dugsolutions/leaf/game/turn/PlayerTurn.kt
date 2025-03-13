package dugsolutions.leaf.game.turn

import dugsolutions.leaf.game.battle.HandleAbsorbDamage
import dugsolutions.leaf.game.battle.HandleDeliverDamage
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.purchase.HandleMarketAcquisition
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.game.turn.handle.HandleGetTarget
import dugsolutions.leaf.game.turn.handle.HandlePassOrPlay
import dugsolutions.leaf.player.Player

class PlayerTurn(
    private val playerRound: PlayerRound,
    private val playerOrder: PlayerOrder,
    private val handleDeliverDamage: HandleDeliverDamage,
    private val handlePlayOrPass: HandlePassOrPlay,
    private val handleGetTarget: HandleGetTarget,
    private val handleMarketAcquisition: HandleMarketAcquisition,
    private val handleAbsorbDamage: HandleAbsorbDamage,
    private val handleCleanup: HandleCleanup
) {

    operator fun invoke(players: List<Player>, phase: GamePhase) {
        prepareForTurn(players)
        var orderedPlayers = playerOrder(players)
        while (playerCanPlay(players)) {
            clearWasHit(players)
            orderedPlayers.forEach { player ->
                if (handlePlayOrPass(player)) {
                    val target = handleGetTarget(player, orderedPlayers)
                    playerRound(player, target)
                }
            }
        }
        prepareForTurn(players)
        orderedPlayers = playerOrder(players)
        if (phase == GamePhase.BATTLE) {
            handleDeliverDamage(orderedPlayers)
            orderedPlayers.forEach { player -> handleAbsorbDamage(player) }
            orderedPlayers.forEach { player -> player.isDormant = false }
        } else {
            orderedPlayers.forEach { player -> handleMarketAcquisition(player) }
        }
        orderedPlayers.forEach { player -> handleCleanup(player) }
    }

    private fun playerCanPlay(players: List<Player>): Boolean {
        return players.any { player -> player.canPlayCard }
    }

    private fun prepareForTurn(players: List<Player>) {
        players.forEach { player ->
            player.hasPassed = false
            player.cardsToPlay.clear()
            player.cardsToPlay.addAll(player.cardsInHand)
        }
    }

    private fun clearWasHit(players: List<Player>) {
        players.forEach { player -> player.wasHit = false }
    }

}
