package dugsolutions.leaf.v35.round.di

import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundDeck
import org.koin.core.module.Module
import org.koin.dsl.module

val roundModule: Module = module {

    single { RoundCardRegistry(get()) }
    single { RoundCardManager() }
    single { RoundDeck(get(), get()) }
}
