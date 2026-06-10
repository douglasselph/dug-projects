package dugsolutions.leaf.v30.round.di

import dugsolutions.leaf.v30.round.RoundCardManager
import dugsolutions.leaf.v30.round.RoundCardRegistry
import dugsolutions.leaf.v30.round.RoundDeck
import org.koin.core.module.Module
import org.koin.dsl.module

val roundModule: Module = module {

    single { RoundCardRegistry() }
    single { RoundCardManager(get()) }
    single { RoundDeck(get(), get()) }
    single { RoundCardsFactory() }
}
