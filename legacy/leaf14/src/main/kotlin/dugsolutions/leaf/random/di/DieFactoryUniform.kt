package dugsolutions.leaf.random.di

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.die.DieUniform
import dugsolutions.leaf.random.die.DieSides

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
