package dugsolutions.leaf.v30.wisp.di

import dugsolutions.leaf.v30.wisp.WispCardManager
import dugsolutions.leaf.v30.wisp.WispCardRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

val wispModule: Module = module {

    single { WispCardRegistry() }
    single { WispCardManager(get()) }
    single { WispCardsFactory() }
}
