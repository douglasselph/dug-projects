package dugsolutions.leaf.game.battle

import dugsolutions.leaf.player.Player

class ProcessBattlePhase(
    private val handleBattleEffects: HandleBattleEffects,
    private val handleDeliverDamage: HandleDeliverDamage,
    private val handleAbsorbDamage: HandleAbsorbDamage
) {

    operator fun invoke(orderedPlayers: List<Player>) {
        orderedPlayers.forEach { player -> handleBattleEffects(player) }
        handleDeliverDamage(orderedPlayers)
        orderedPlayers.forEach { player -> handleAbsorbDamage(player) }
        orderedPlayers.forEach { player -> player.isDormant = false }
    }
}
