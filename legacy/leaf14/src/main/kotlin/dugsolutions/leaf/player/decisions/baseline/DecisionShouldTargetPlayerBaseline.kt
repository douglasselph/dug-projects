package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldTargetPlayer

class DecisionShouldTargetPlayerBaseline(
    private val player: Player
) : DecisionShouldTargetPlayer {

    override fun invoke(target: Player, amount: Int): Boolean {
        // If any die in hand can be adjusted by amount without exceeding max, return false
        return !player.diceInHand.any { die ->
            die.value + amount <= die.sides
        }
    }

}
