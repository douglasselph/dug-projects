package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.turn.select.SelectPossibleDice

class PossibleBestDice(
    private val selectPossibleDice: SelectPossibleDice,
    private val dieCost: DieCost
) {

    operator fun invoke(
        combinations: Combinations
    ): List<ChoiceDie> {
        val marketDice = selectPossibleDice()
        val choices = mutableListOf<ChoiceDie>()

        for (combination in combinations) {
            val totalValue = calculateTotalValue(combination)
            val possible = marketDice
                .filter { die -> dieCost(die) <= totalValue }
                .maxByOrNull { die -> dieCost(die) }
            possible?.let {
                choices.add(ChoiceDie(die = it, combination = combination))
            }
        }
        return choices
    }

    private fun calculateTotalValue(combination: Combination): Int {
        // Sum all dice values
        val diceTotal = combination.values.dice.sumOf { it.value }

        // Add any additional total from effects
        return diceTotal + combination.addToTotal
    }
}
