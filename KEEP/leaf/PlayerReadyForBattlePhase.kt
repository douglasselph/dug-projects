package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player

class PlayerReadyForBattlePhase(
    private val grove: Grove
) {
    operator fun invoke(player: Player, isAbleToDoBattlePhase: Boolean): Boolean {
        if (player.isDormant) {
            player.isDormant = false
            return true
        }
        if (player.bonusDie != null) {
            return true
        }
        if (isAbleToDoBattlePhase) {
            player.bonusDie = grove.useNextBonusDie
            player.reset()
            player.isDormant = true
            return true
        }
        return false
    }
}
