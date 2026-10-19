package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectDieToMax
import dugsolutions.leaf.v14.player.Player

class EffectDieToMax(
    private val selectDieToMax: SelectDieToMax,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        selectDieToMax(player.diceInHand)?.let { die ->
            val amount = die.adjustToMax()
            chronicle(Moment.ADJUST_DIE(player, die, amount))
        }
    }

}
