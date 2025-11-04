package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectDieToMax
import dugsolutions.leaf.player.Player

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
