package dugsolutions.leaf.v30.cards.di

import dugsolutions.leaf.v30.cards.CheckGameCardName
import dugsolutions.leaf.v30.cards.CheckGameCardNames
import dugsolutions.leaf.v30.cards.GameCardManager
import dugsolutions.leaf.v30.cards.GameCardRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

val cardsModule: Module = module {

    single { GameCardRegistry(get()) }
    single { CheckGameCardName(get()) }
    single { CheckGameCardNames(get()) }
    single { GameCardManager(get(), get()) }
    single { GameCardsFactory() }

} 
