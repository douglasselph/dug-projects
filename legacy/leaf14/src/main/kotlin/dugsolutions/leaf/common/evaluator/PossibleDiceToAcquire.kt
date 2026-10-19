package dugsolutions.leaf.common.evaluator

import dugsolutions.leaf.common.domain.acquire.ChoiceDie
import dugsolutions.leaf.random.die.DieCost
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.DieValues

class PossibleDiceToAcquire(
    private val dieCost: DieCost
) {

    operator fun invoke(
        combinations: List<DieValues>,
        marketDice: List<DieSides>
    ): List<ChoiceDie> {
        val choices = mutableListOf<ChoiceDie>()

        for (combination in combinations) {
            val possible = marketDice
                .filter { die -> dieCost(die.value) <= combination.total }
                .maxByOrNull { die -> dieCost(die.value) }
            possible?.let {
                addIfBetter(choices, ChoiceDie(dieSides = it, usingDice = combination))
            }
        }
        return choices.sortedWith(
            compareBy<ChoiceDie> { it.dieSides }
                .thenBy { it.usingDice.total }
        )
    }

    private fun addIfBetter(choices: MutableList<ChoiceDie>, newChoice: ChoiceDie) {
        val newChoiceValue = newChoice.usingDice.total
        val sameSidedDice = choices.filter { it.dieSides == newChoice.dieSides }
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
