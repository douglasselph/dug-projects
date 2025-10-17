package dugsolutions.leaf.grove.di

import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.GroveStacks
import dugsolutions.leaf.grove.local.GameCardsUseCase
import dugsolutions.leaf.grove.local.GroveNearingTransition
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
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
