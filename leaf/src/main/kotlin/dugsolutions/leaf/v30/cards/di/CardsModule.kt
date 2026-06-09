package dugsolutions.leaf.v30.cards.di

import dugsolutions.leaf.v30.cards.GameCardManager
import dugsolutions.leaf.v30.cards.GameCardRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

val cardsModule: Module = module {

    single { GameCardRegistry() }
    single { GameCardManager(get()) }
    single { GameCardsFactory() }

} 
