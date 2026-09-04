package dugsolutions.leaf.v35.round.di

import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Registry/manager are shared card-definition services.
 *
 * RoundDeck is mutable per-game state, so GameFactory constructs it with the
 * Game's Randomizer instead of Koin registering a singleton deck.
 */
val roundModule: Module = module {
    single {
        RoundCardRegistry(get())
    }

    single {
        RoundCardManager()
    }
}
