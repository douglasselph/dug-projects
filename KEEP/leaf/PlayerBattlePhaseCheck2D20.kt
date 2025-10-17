package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player

class PlayerBattlePhaseCheck2D20(
    private val grove: Grove,
    private val dieFactory: DieFactory
) : PlayerBattlePhaseCheck {

    override fun isReady(player: Player): Boolean {
        if (player.allDice.dice.count { it.sides == 20 } >= 2) {
            return true
        }
        if (!grove.hasCards && !grove.hasDice) {
            return true
        }
        return false
    }

    override fun giftTo(player: Player) {
        if (!isReady(player)) {
            if (grove.hasDie(DieSides.D20.value)) {
                val die = dieFactory(DieSides.D20)
                grove.removeDie(die)
                player.addDieToCompost(die)
            }
        }
    }

}
