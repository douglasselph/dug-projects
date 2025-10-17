package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.common.domain.acquire.ChoiceDie


class AcquireDieEvaluator {

    operator fun invoke(
        possibleDice: List<ChoiceDie>
    ): ChoiceDie? {
        // Find the choice with the die that has the most sides
        // If multiple choices have the same die sides, pick the one with fewest die values
        val bestChoice = possibleDice.maxWithOrNull(compareBy<ChoiceDie> { it.die.sides }
            .thenBy { -it.usingDice.values.dice.size }) // Negative to prioritize smaller lists
        return bestChoice
    }
}
