package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.random.die.Die

class SelectDieToReroll {

    /**
     * Selects a die for rerolling based on its current value relative to its maximum value (sides).
     * Prioritizes dice with larger numbers of sides.
     *
     * Selection criteria (in order):
     * 1. Die less than 1/3 of its maximum value
     * 2. Die less than 1/2 of its maximum value
     * 3. Skip if no dice meet criteria
     *
     * @param dice The collection of dice to choose from
     * @return The selected die for rerolling, or null if no suitable die found
     */
    operator fun invoke(dice: List<Die>): Die? {
        if (dice.isEmpty()) {
            return null
        }
        // First try to find dice less than 1/3 of their max value
        val underThird = dice.filter { it.value < it.sides / 3 }
        if (underThird.isNotEmpty()) {
            return getLargestDistance(underThird)
        }
        // Then try to find dice less than 1/2 of their max value
        val underHalf = dice.filter { it.value < it.sides / 2 }
        if (underHalf.isNotEmpty()) {
            return getLargestDistance(underHalf)
        }
        // No suitable dice found
        return null
    }

    private fun getLargestDistance(dice: List<Die>): Die? {
        return dice.maxByOrNull { it.sides - it.value }
    }
}
