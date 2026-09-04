package dugsolutions.leaf.v35.game.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Only the stateless GameFactory is registered application-wide.
 *
 * Game, Grove, Chronicle, RoundDeck, Randomizer, Players, and BattleState are
 * all per-game/per-round state and are not registered here.
 */
val gameModule: Module = module {
    single {
        GameFactory(
            groveFactory = get(),
            roundCardManager = get()
        )
    }
}
