package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.turn.select.SelectPossibleDice

class AcquireDieEvaluator(
    private val selectPossibleDice: SelectPossibleDice,
    private val evaluateBestDiePurchase: EvaluateBestDiePurchase
) {

    data class BestChoice(
        val die: Die,
        val combination: Combination
    )

    operator fun invoke(
        combinations: Combinations
    ): BestChoice? {
        val marketDice = selectPossibleDice()
        val bestChoices = mutableListOf<BestChoice>()
        
        for (combination in combinations) {
            evaluateBestDiePurchase(marketDice, combination)?.let { die ->
                bestChoices.add(
                    BestChoice(
                        die,
                        combination
                    )
                )
            }
        }
        
        if (bestChoices.isEmpty()) {
            return null
        }
        
        // Find the choice with the die that has the most sides
        // If multiple choices have the same die sides, pick the one with fewest die values
        val bestChoice = bestChoices.maxWithOrNull(compareBy<BestChoice> { it.die.sides }
            .thenBy { -it.combination.values.dice.size }) // Negative to prioritize smaller lists
        
        return bestChoice
    }
}
