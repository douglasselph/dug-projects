package dugsolutions.leaf.v14.di

import dugsolutions.leaf.v14.cards.di.cardModule
import dugsolutions.leaf.v14.chronicle.di.chronicleModule
import dugsolutions.leaf.v14.game.di.gameModule
import dugsolutions.leaf.v14.grove.di.groveModule
import dugsolutions.leaf.v14.main.di.mainModule
import dugsolutions.leaf.v14.player.di.playerModule
import dugsolutions.leaf.v14.random.di.randomModule
import kotlinx.coroutines.Dispatchers
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
