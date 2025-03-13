package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die

class SelectDieToAdjust {

    /**
     * Selects a die to adjust based on the given criteria:
     * 1. First die that can be adjusted by the indicated amount without going over max or less than min
     * 2. If no die can be adjusted by the full amount, select the die that can be adjusted the most
     *
     * @param dice List of dice to choose from
     * @param adjustment The adjustment amount to be applied
     * @return The selected die, or null if no suitable die found
     */
    operator fun invoke(dice: Dice, adjustment: Int): Die? {
        val list = dice.dice
        if (list.isEmpty()) return null
        if (adjustment == 0) return null

        if (adjustment > 0) {
            // First try to find a die that can be adjusted by the full amount
            val dieWithFullAdjustment = list.firstOrNull {
                it.value + adjustment <= it.sides
            }
            if (dieWithFullAdjustment != null) return dieWithFullAdjustment

            // If no die can be adjusted by the full amount, find the one that can be increased the most
            return list.maxByOrNull { it.sides - it.value }
        } else {
            val dieWithFullAdjustment = list.firstOrNull {
                it.value + adjustment >= 1
            }
            if (dieWithFullAdjustment != null) return dieWithFullAdjustment

            // If no die can be adjusted by the full amount, find the one that can be increased the most
            return list.minByOrNull { it.sides - it.value }
        }
    }
}
