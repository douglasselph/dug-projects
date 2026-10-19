package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.common.domain.acquire.ChoiceDie


class EvaluateAcquireDie {

    operator fun invoke(
        possibleDice: List<ChoiceDie>
    ): ChoiceDie? {
        // Find the choice with the die that has the most sides
        // If multiple choices have the same die sides, pick the one with fewest die values
        val bestChoice = possibleDice.maxWithOrNull(compareBy<ChoiceDie> { it.dieSides }
            .thenBy { -it.usingDice.dice.size }) // Negative to prioritize smaller lists
        return bestChoice
    }
}
