package dugsolutions.leaf.random.di

import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.RandomizerDefault
import dugsolutions.leaf.random.die.DieCost
import org.koin.core.module.Module
import org.koin.dsl.module

val randomModule: Module = module {

    single { DieCost() }

    single<Randomizer> { RandomizerDefault() }
    single { DieFactory(get()) }

}
