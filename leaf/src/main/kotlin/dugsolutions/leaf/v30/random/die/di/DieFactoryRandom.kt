package dugsolutions.leaf.v30.random.die.di

import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Die
import dugsolutions.leaf.v30.random.die.DieRandom
import dugsolutions.leaf.v30.random.die.DieSides


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
