package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.v14.player.Player

class  EffectDieToRetain(
    private val selectDieToRetain: SelectDieToRetain,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, withReroll: Boolean = false) {
        selectDieToRetain(player.diceInHand)?.let { die ->
            player.retainDie(die)
            var oldValue: Int? = die.value
            if (withReroll) {
                die.roll()
            } else {
                oldValue = null
            }
            chronicle(Moment.RETAIN_DIE(player, die, oldValue))
        }
    }

}
