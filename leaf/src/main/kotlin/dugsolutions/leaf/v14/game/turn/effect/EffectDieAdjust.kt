package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.v14.player.Player

class EffectDieAdjust(
    private val selectDieToAdjust: SelectDieToAdjust,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, value: Int, target: Player? = null) {
        if (target != null && player.decisionDirector.shouldTargetPlayer(target, value)) {
            selectDieToAdjust(target.diceInHand, -value)?.let { die ->
                target.diceInHand.adjust(die, -value)
                chronicle(Moment.ADJUST_DIE(target, die, -value))
            }
        } else {
            addToDie(player, value)
        }
    }

    private fun addToDie(player: Player, value: Int) {
        selectDieToAdjust(player.diceInHand, value)?.let { selectedDie ->
            if (player.diceInHand.adjust(selectedDie, value)) {
                chronicle(
                    Moment.ADJUST_DIE(player, selectedDie, value)
                )
            }
        }
    }
}
