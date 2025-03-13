package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.tool.Randomizer

class SelectDieToRetain(
    private val randomizer: Randomizer
) {
    operator fun invoke(dice: Dice): Die? {
        // 50% chance to return null
        if (randomizer.nextBoolean()) {
            return null
        }
        // If we have dice, randomly select one
        return randomizer.randomOrNull(dice.dice)
    }
}
