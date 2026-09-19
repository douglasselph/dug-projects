package dugsolutions.leaf.v14.game.turn.select

import dugsolutions.leaf.v14.random.die.Dice
import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.Randomizer

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
