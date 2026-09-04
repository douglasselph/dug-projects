package dugsolutions.leaf.v35.wisp.di

import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Wisp card definitions are application-wide.
 *
 * WispDeck is mutable per-game state and is created by GroveFactory with the
 * owning Game's Randomizer.
 */
val wispModule: Module = module {
    single { WispCardRegistry(get()) }
    single { WispCardManager() }
}
