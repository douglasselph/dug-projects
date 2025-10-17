package dugsolutions.leaf.cards.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.CardRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

val cardModule: Module = module {

    single { CardRegistry() }
    single { CardManager(get()) }
    single { GameCardsFactory(get()) }

} 
