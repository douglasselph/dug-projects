package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectDiceNotActivatingMatches
import dugsolutions.leaf.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.player.Player

/**
 * REROLL_ACCEPT_2ND("Reroll VALUE dice, you must accept the second roll")
 *   Ignore die which are causing match.
 *   Otherwise if a die is low enough do now.
 * REROLL_TAKE_BETTER("Reroll VALUE dice, you may take the better of the two rolls")
 *   Ignore die which are causing match
 */
class EffectDieReroll(
    private val selectDieToReroll: SelectDieToReroll,
    private val selectDiceNotActivatingMatches: SelectDiceNotActivatingMatches,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player, takeBetter: Boolean) {
        val useDice = if (takeBetter) player.diceInHand.dice else selectDiceNotActivatingMatches(player)
        selectDieToReroll(useDice)?.let { die ->
            val beforeValue = die.value
            val newValue = die.roll().value
            if (takeBetter) {
                if (beforeValue > newValue) {
                    die.adjustTo(beforeValue)
                }
            }
            chronicle(Moment.REROLL(player, die, beforeValue))
        }
    }

}
