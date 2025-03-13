package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.market.Market
import dugsolutions.leaf.player.Player

class PlayerSetupForBattlePhase(
    private val market: Market
) {
    operator fun invoke(
        player: Player,
        playerBattlePhaseCheck: PlayerBattlePhaseCheck
    ) {
        player.bonusDie?.let {
            player.addDieToCompost(it)
            player.bonusDie = null
        } ?: run {
            market.useNextBonusDie?.let { player.addDieToCompost(it) }
            playerBattlePhaseCheck.giftTo(player)
            player.isDormant = true
        }
        player.trashSeedlingCards()
        player.reset()
        player.drawHand()
    }

}
