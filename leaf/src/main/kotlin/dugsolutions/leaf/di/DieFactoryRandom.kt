package dugsolutions.leaf.di

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.tool.Randomizer
import dugsolutions.leaf.components.die.DieRandom
import dugsolutions.leaf.components.die.DieSides

class DieFactoryRandom(
    private val randomizer: Randomizer
) : DieFactory {
    override fun invoke(sides: DieSides): Die {
        return DieRandom(sides.value, randomizer).roll()
    }

    override fun invoke(sides: Int): Die {
        return DieRandom(sides, randomizer).roll()
    }


}