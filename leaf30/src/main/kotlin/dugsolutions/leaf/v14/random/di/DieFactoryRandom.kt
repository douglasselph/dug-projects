package dugsolutions.leaf.v14.random.di

import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.Randomizer
import dugsolutions.leaf.v14.random.die.DieRandom
import dugsolutions.leaf.v14.random.die.DieSides

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
