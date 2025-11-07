package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.common.domain.acquire.ChoiceDie
import dugsolutions.leaf.grove.SelectPossibleDice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieCost
import dugsolutions.leaf.random.die.DieValues

class PossibleDiceToAcquire(
    private val dieCost: DieCost
) {

    operator fun invoke(
        combinations: List<DieValues>,
        marketDice: List<Die>
    ): List<ChoiceDie> {
        val choices = mutableListOf<ChoiceDie>()

        for (combination in combinations) {
            val possible = marketDice
                .filter { die -> dieCost(die) <= combination.total }
                .maxByOrNull { die -> dieCost(die) }
            possible?.let {
                addIfBetter(choices, ChoiceDie(die = it, usingDice = combination))
            }
        }
        return choices.sortedWith(
            compareBy<ChoiceDie> { it.die.sides }
                .thenBy { it.usingDice.total }
        )
    }

    private fun addIfBetter(choices: MutableList<ChoiceDie>, newChoice: ChoiceDie) {
        val newChoiceValue = newChoice.usingDice.total
        val sameSidedDice = choices.filter { it.die.sides == newChoice.die.sides }
        val isBetter = sameSidedDice.all { it.usingDice.total >= newChoiceValue }
        if (isBetter) {
            // Find the element with the largest gap
            val elementToRemove = sameSidedDice.maxByOrNull { choice -> 
                choice.usingDice.total - newChoiceValue
            }
            // Remove it if found
            elementToRemove?.let { choices.remove(it) }
            // Add the new choice
            choices.add(newChoice)
        }
    }
}
