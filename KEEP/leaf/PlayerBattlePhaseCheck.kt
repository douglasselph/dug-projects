package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.player.Player

interface PlayerBattlePhaseCheck {
    fun isReady(player: Player): Boolean
    fun giftTo(player: Player)

}