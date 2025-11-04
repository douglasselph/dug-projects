package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.DieValues

class GenerateCombinations {

    operator fun invoke(dieValues: DieValues): Combinations {
        return Combinations(generateCombinations(dieValues))
    }

    private fun generateCombinations(
        dieValues: DieValues
    ): List<Combination> {
        val result = mutableListOf<Combination>()
        val dice = dieValues.dice

        // Generate all possible combinations using binary counting
        // For n dice, we need 2^n - 1 combinations (excluding empty set)
        val numCombinations = (1 shl dice.size) - 1

        for (i in 1..numCombinations) {
            val selectedDice = mutableListOf<DieValue>()

            // Check each bit in the number
            for (j in dice.indices) {
                // If the j-th bit is set, include that die
                if ((i and (1 shl j)) != 0) {
                    selectedDice.add(dice[j])
                }
            }

            // Create a new combination with the selected dice
            result.add(Combination(values = DieValues(selectedDice)))
        }
        return result
    }

}
