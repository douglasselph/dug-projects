package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die

class SelectDieToMax {

    /**
     * Selects the die that has the greatest difference between its current value and maximum value (sides).
     *
     * @param dice The collection of dice to choose from
     * @return The die with the greatest potential for increase, or null if no dice available
     */
    operator fun invoke(dice: Dice): Die? {
        val diceList = dice.dice
        
        if (diceList.isEmpty()) {
            return null
        }
        
        // Sort dice by the difference between max value (sides) and current value (descending)
        return diceList.maxByOrNull { it.sides - it.value }
    }
}
