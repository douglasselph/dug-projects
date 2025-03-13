package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player

class PlayerReadyForBattlePhase(
    private val market: Market
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
            player.bonusDie = market.useNextBonusDie
            player.reset()
            player.isDormant = true
            return true
        }
        return false
    }
}