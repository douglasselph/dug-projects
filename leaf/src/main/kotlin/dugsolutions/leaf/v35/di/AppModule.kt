package dugsolutions.leaf.v35.di

import dugsolutions.leaf.v35.effect.di.effectModule
import dugsolutions.leaf.v35.game.di.gameModule
import dugsolutions.leaf.v35.grove.di.groveModule
import dugsolutions.leaf.v35.plant.di.plantModule
import dugsolutions.leaf.v35.round.di.roundModule
import dugsolutions.leaf.v35.wisp.di.wispModule
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module = module {
    single { Dispatchers.Main }
    single { Dispatchers.IO }
}

/**
 * Application-wide services/catalogs/factories only.
 *
 * Mutable per-game state (Randomizer, DieFactory, WispDeck, Grove, Players,
 * RoundDeck, Chronicle, Game) is created by GameFactory rather than Koin.
 */
val appModules = listOf(
    appModule,
    effectModule,
    plantModule,
    wispModule,
    roundModule,
    groveModule,
    gameModule
)
