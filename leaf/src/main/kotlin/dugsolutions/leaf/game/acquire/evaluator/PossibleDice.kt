package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.random.die.DieCost
import dugsolutions.leaf.game.acquire.domain.ChoiceDie
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.turn.select.SelectPossibleDice

class PossibleDice(
    private val selectPossibleDice: SelectPossibleDice,
    private val dieCost: DieCost
) {

    operator fun invoke(
        combinations: Combinations
    ): List<ChoiceDie> {
        val marketDice = selectPossibleDice()
        val choices = mutableListOf<ChoiceDie>()

        for (combination in combinations) {
            val totalValue = combination.totalValue
            val possible = marketDice
                .filter { die -> dieCost(die) <= totalValue }
                .maxByOrNull { die -> dieCost(die) }
            possible?.let {
                addIfBetter(choices, ChoiceDie(die = it, combination = combination))
            }
        }
        return choices.sortedWith(
            compareBy<ChoiceDie> { it.die.sides }
                .thenBy { it.combination.totalValue }
        )
    }

    private fun addIfBetter(choices: MutableList<ChoiceDie>, newChoice: ChoiceDie) {
        val newChoiceValue = newChoice.combination.totalValue
        val sameSidedDice = choices.filter { it.die.sides == newChoice.die.sides }
        val isBetter = sameSidedDice.all { it.combination.totalValue >= newChoiceValue }
        if (isBetter) {
            // Find the element with the largest gap
            val elementToRemove = sameSidedDice.maxByOrNull { choice -> 
                choice.combination.totalValue - newChoiceValue 
            }
            // Remove it if found
            elementToRemove?.let { choices.remove(it) }
            // Add the new choice
            choices.add(newChoice)
        }
    }
}
