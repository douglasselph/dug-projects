package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.select.SelectDieAnyToReroll
import dugsolutions.leaf.v14.player.Player

/**
 * REROLL_ANY - Reroll one of your own or an opponents die. You must accept the 2nd roll.
 */
class EffectDieRerollAny(
    private val selectDieAnyToReroll: SelectDieAnyToReroll,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, target: Player) {
        val best = selectDieAnyToReroll(player, target)
        best.playerDie?.let { die ->
            val beforeValue = die.value
            chronicle(Moment.REROLL(player, die, beforeValue))
        } ?: best.opponentDie?.let { die ->
            val beforeValue = die.value
            chronicle(Moment.REROLL(target, die, beforeValue))
        }
    }

}
