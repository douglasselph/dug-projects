package dugsolutions.leaf.game.purchase.evaluator

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.purchase.domain.Combination

class EvaluateBestDiePurchase(
    private val dieCost: DieCost
) {

    operator fun invoke(
        marketDice: List<Die>,
        combination: Combination
    ): Die? {
        // Calculate the total value available for purchase
        val totalValue = calculateTotalValue(combination)
        
        // Filter dice that can be afforded, then find the most expensive one
        return marketDice
            .filter { die -> dieCost(die) <= totalValue }
            .maxByOrNull { die -> dieCost(die) }
    }
    
    private fun calculateTotalValue(combination: Combination): Int {
        // Sum all dice values
        val diceTotal = combination.values.dice.sumOf { it.value }
        
        // Add any additional total from effects
        return diceTotal + combination.addToTotal
    }
}
