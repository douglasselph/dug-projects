package dugsolutions.leaf.di.factory

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides

interface DieFactoryImpl {

    operator fun invoke(sides: DieSides): Die
    operator fun invoke(sides: Int): Die

}
