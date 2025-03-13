package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.player.Player

interface IsEliminated {
    operator fun invoke(player: Player): Boolean

}