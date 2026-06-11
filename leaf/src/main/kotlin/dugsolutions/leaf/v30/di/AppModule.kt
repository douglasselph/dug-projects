package dugsolutions.leaf.v30.di

import dugsolutions.leaf.v30.cards.di.cardsModule
import dugsolutions.leaf.v30.battle.di.battleModule
import dugsolutions.leaf.v30.chronicle.di.chronicleModule
import dugsolutions.leaf.v30.game.di.gameModule
import dugsolutions.leaf.v30.grove.di.groveModule
import dugsolutions.leaf.v30.player.di.playerModule
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.round.di.roundModule
import dugsolutions.leaf.v30.table.di.tableModule
import dugsolutions.leaf.v30.wisp.di.wispModule
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module = module {

    single { Dispatchers.Main }
    single { Dispatchers.IO }
    single<Randomizer> { Randomizer.create() }
}

val appModules = listOf(
    appModule,
    cardsModule,
    wispModule,
    groveModule,
    roundModule,
    chronicleModule,
    battleModule,
    tableModule,
    playerModule,
    gameModule
)
