package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.player.Player

class DecisionRerollOneDieCoreStrategy(
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
