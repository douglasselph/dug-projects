package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.Randomizer

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
