package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.player.Player

class DecisionShouldTargetPlayerCoreStrategy(
    private val player: Player
) : DecisionShouldTargetPlayer {

    override fun invoke(target: Player, amount: Int): Boolean {
        // If any die in hand can be adjusted by amount without exceeding max, return false
        return !player.diceInHand.any { die ->
            die.value + amount <= die.sides
        }
    }

}
