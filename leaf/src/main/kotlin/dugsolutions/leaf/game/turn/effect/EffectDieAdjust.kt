package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.player.Player

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
