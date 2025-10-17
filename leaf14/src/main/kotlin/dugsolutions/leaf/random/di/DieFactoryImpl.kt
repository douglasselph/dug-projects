package dugsolutions.leaf.random.di

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides

interface DieFactoryImpl {

    operator fun invoke(sides: DieSides): Die
    operator fun invoke(sides: Int): Die

}
