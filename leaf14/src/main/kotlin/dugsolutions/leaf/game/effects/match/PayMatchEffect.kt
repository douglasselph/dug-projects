package dugsolutions.leaf.game.effects.match

import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.player.Player

class PayMatchEffect {

    operator fun invoke(player: Player, token: Token): Boolean {
        when (token) {
            Token.None -> return true
            Token.Bee,
            Token.Worm,
            Token.Ladybug -> {
                if (player.hasBug(token)) {
                    player.removeBug(token)
                    return true
                }
            }
            Token.Sap -> {
                if (player.hasSap) {
                    player.useSap()
                    return true
                }
            }
            Token.Aphid -> return false
        }
        return false
    }
}
