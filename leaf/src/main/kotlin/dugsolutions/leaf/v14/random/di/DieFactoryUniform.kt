package dugsolutions.leaf.v14.random.di

import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.Randomizer
import dugsolutions.leaf.v14.random.die.DieUniform
import dugsolutions.leaf.v14.random.die.DieSides

class DieFactoryUniform(
    private val randomizer: Randomizer
) : DieFactoryImpl {
    override fun invoke(sides: DieSides): Die {
        return DieUniform(sides.value, randomizer).roll()
    }

    override fun invoke(sides: Int): Die {
        return DieUniform(sides, randomizer).roll()
    }


}
