package dugsolutions.leaf.game.battle

import dugsolutions.leaf.game.battle.domain.PlayerValues
import dugsolutions.leaf.player.Player

class HandleInsects {

    /**
     * For each die in the player's hand, determine if an insect should accompany it to boost it's value.
     */
    suspend operator fun invoke(player: Player): PlayerValues {
        return PlayerValues(player)
    }

}
