package dugsolutions.leaf.common.evaluator

import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.DieValues

class GenerateCombinations {

    operator fun invoke(dieValues: DieValues): List<DieValues> {
        val result = mutableListOf<DieValues>()
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
            result.add(DieValues(selectedDice))
        }
        return result
    }

}
