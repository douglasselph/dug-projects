package dugsolutions.leaf.components.die

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.tool.Randomizer

class DieRandom(
    sides: Int,
    private val randomizer: Randomizer
) : Die(sides) {
    override fun roll(): Die {
       _value = randomizer.nextInt(1, sides+1)
        return this
    }

}