package dugsolutions.leaf.v35.wisp.di

import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.wisp.WispDeck
import org.koin.core.module.Module
import org.koin.dsl.module

val wispModule: Module = module {

    single { WispCardRegistry(get()) }
    single { WispCardManager() }
    single { WispDeck(get(), get()) }
}
