package dugsolutions.leaf.v35.random.die.di

import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides

interface DieFactoryImpl {

    operator fun invoke(sides: DieSides): Die
    operator fun invoke(sides: Int): Die

}
