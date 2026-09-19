package dugsolutions.leaf.v14.random.di

import dugsolutions.leaf.v14.random.die.Die
import dugsolutions.leaf.v14.random.die.DieSides

interface DieFactoryImpl {

    operator fun invoke(sides: DieSides): Die
    operator fun invoke(sides: Int): Die

}
