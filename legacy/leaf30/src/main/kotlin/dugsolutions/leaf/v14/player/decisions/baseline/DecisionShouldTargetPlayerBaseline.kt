package dugsolutions.leaf.v14.player.decisions.baseline

import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.decisions.core.DecisionShouldTargetPlayer

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
