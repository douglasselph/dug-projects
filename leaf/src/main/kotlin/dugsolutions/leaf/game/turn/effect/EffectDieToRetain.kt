package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.player.Player

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
