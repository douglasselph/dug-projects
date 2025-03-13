package dugsolutions.leaf.di

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.tool.Randomizer
import dugsolutions.leaf.components.die.DieUniform
import dugsolutions.leaf.components.die.DieSides

class DieFactoryUniform(
    private val randomizer: Randomizer
) : DieFactory {
    override fun invoke(sides: DieSides): Die {
        return DieUniform(sides.value, randomizer).roll()
    }

    override fun invoke(sides: Int): Die {
        return DieUniform(sides, randomizer).roll()
    }


}