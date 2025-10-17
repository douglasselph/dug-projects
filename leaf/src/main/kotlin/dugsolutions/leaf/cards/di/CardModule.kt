package dugsolutions.leaf.cards.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.CardRegistry
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.cost.ParseCost
import dugsolutions.leaf.cards.cost.ParseCostElement
import dugsolutions.leaf.main.local.CardOperations
import org.koin.core.module.Module
import org.koin.dsl.module

val cardModule: Module = module {

    single { CostScore() }
    single { ParseCostElement() }
    single { ParseCost(get()) }

    single { CardRegistry(get()) }
    single { CardManager(get()) }
    single { GameCardIDsFactory(get(), get()) }
    single { GameCardsFactory(get(), get()) }
    single { CardOperations(get(), get(), get()) }


} 
