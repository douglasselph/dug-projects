package dugsolutions.leaf.v35.di

import dugsolutions.leaf.v35.plant.di.plantModule
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.di.DieFactory
import dugsolutions.leaf.v35.round.di.roundModule
import dugsolutions.leaf.v35.wisp.di.wispModule
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module = module {

    single { Dispatchers.Main }
    single { Dispatchers.IO }
    single<Randomizer> { Randomizer.create() }
    single { DieFactory(get()) }
}

val appModules = listOf(
    appModule,
    plantModule,
    wispModule,
    roundModule
)
