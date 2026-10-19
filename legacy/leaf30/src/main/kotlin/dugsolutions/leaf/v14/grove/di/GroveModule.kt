package dugsolutions.leaf.v14.grove.di

import dugsolutions.leaf.v14.grove.Grove
import dugsolutions.leaf.v14.grove.domain.GroveStacks
import dugsolutions.leaf.v14.grove.local.GameCardsUseCase
import dugsolutions.leaf.v14.grove.local.GroveNearingTransition
import dugsolutions.leaf.v14.grove.scenario.ScenarioBasicConfig
import org.koin.core.module.Module
import org.koin.dsl.module

val groveModule: Module = module {

    single { ScenarioBasicConfig(get()) }

    single {
        GroveStacks(
            cardManager = get(),
            gameCardIDsFactory = get()
        )
    }

    single { GroveNearingTransition(get()) }
    single { GameCardsUseCase(get()) }

    single { Grove(get(), get()) }

}
