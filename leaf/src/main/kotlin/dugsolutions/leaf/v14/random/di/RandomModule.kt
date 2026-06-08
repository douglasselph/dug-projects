package dugsolutions.leaf.v14.random.di

import dugsolutions.leaf.v14.random.Randomizer
import dugsolutions.leaf.v14.random.RandomizerDefault
import dugsolutions.leaf.v14.random.die.DieCost
import org.koin.dsl.module

val randomModule: Module = module {

    single { DieCost() }

    single<Randomizer> { RandomizerDefault() }
    single { DieFactory(get()) }

}
