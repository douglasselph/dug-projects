package dugsolutions.leaf.game.turn.local

import dugsolutions.leaf.game.purchase.credit.CombinationGenerator
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.game.purchase.domain.totalValue
import dugsolutions.leaf.player.Player

class EvaluateSimpleCost(
    private val combinationGenerator: CombinationGenerator
) {

    operator fun invoke(player: Player, amount: Int): Combination? {
        val combinations = combinationGenerator(player)
        // Filter combinations that meet or exceed the amount
        val validCombinations = combinations.filter { it.totalValue >= amount }
        // Find the one with the smallest difference to the amount
        return validCombinations.minByOrNull { it.totalValue - amount }
    }

}
