package dugsolutions.leaf.game.battle

import dugsolutions.leaf.game.battle.domain.PlayerValues
import dugsolutions.leaf.player.Player

class HandleInsects {

    suspend operator fun invoke(player: Player): PlayerValues {
        return PlayerValues(player)
    }

}
