package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.common.domain.acquire.UsingDice
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.random.die.DieValues

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.DieValue

class CombinationGenerator {

    operator fun invoke(player: Player): Combinations {
        val dieList = player.diceInHand.dice
        val addToTotal = player.pipModifier
        val dieValues = DieValues.from(dieList)
        val combinations = mutableListOf<UsingDice>()

        combinations.addAll(generateCombinations(dieValues))
        combinations.addAll(extendCombinations(combinations, addToTotal))

        return Combinations(combinations)
    }

    private fun generateCombinations(
        dieValues: DieValues
    ): List<UsingDice> {
        val result = mutableListOf<UsingDice>()
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
            result.add(UsingDice(values = DieValues(selectedDice)))
        }
        return result
    }

    private fun extendCombinations(
        combinations: List<UsingDice>,
        addToTotal: Int
    ): List<UsingDice> {
        if (addToTotal <= 0) return emptyList()
        return combinations.map { combination ->
            combination.copy(addToTotal = addToTotal)
        }
    }

}
