package dugsolutions.leaf.di

import dugsolutions.leaf.cards.di.cardModule
import dugsolutions.leaf.chronicle.di.chronicleModule
import dugsolutions.leaf.game.di.gameModule
import dugsolutions.leaf.grove.di.groveModule
import dugsolutions.leaf.main.di.mainModule
import dugsolutions.leaf.player.di.playerModule
import dugsolutions.leaf.random.di.randomModule
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module = module {

    single { Dispatchers.Main }
    single { Dispatchers.IO }
}

val appModules = listOf(
    appModule,
    cardModule,
    chronicleModule,
    gameModule,
    groveModule,
    mainModule,
    playerModule,
    randomModule
)
