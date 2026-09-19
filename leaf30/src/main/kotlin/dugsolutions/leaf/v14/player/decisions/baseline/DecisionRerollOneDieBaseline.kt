package dugsolutions.leaf.v14.player.decisions.baseline

import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.decisions.core.DecisionRerollOneDie

class DecisionRerollOneDieBaseline(
    private val player: Player
) : DecisionRerollOneDie {

    override fun invoke(): Boolean {
        // Find the die with the greatest potential improvement
        val dieToReroll = player.diceInHand.dice.maxByOrNull { die ->
            die.sides - die.value
        } ?: return false

        // Reroll the chosen die
        dieToReroll.roll()

        return true
    }

}
