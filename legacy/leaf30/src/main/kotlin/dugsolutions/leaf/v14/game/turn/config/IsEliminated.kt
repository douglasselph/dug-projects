package dugsolutions.leaf.v14.game.turn.config

import dugsolutions.leaf.v14.player.Player

interface IsEliminated {
    operator fun invoke(player: Player): Boolean

}