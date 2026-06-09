package dugsolutions.leaf.v30.random.die

import dugsolutions.leaf.v30.random.Randomizer


class DieRandom(
    sides: Int,
    private val randomizer: Randomizer
) : Die(sides) {
    override fun roll(): Die {
       _value = randomizer.nextInt(1, sides+1)
        return this
    }

}
