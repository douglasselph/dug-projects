package dugsolutions.leaf.random.di

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.die.DieRandom
import dugsolutions.leaf.random.die.DieSides

class DieFactoryRandom(
    private val randomizer: Randomizer
) : DieFactoryImpl {

    override fun invoke(sides: DieSides): Die {
        return DieRandom(sides.value, randomizer).roll()
    }

    override fun invoke(sides: Int): Die {
        return DieRandom(sides, randomizer).roll()
    }

}
