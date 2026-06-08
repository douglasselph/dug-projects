package dugsolutions.leaf.v14.cards.di

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.CardRegistry
import dugsolutions.leaf.v14.cards.cost.CostScore
import dugsolutions.leaf.v14.cards.cost.ParseCost
import dugsolutions.leaf.v14.cards.cost.ParseCostElement
import dugsolutions.leaf.v14.main.local.CardOperations
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
