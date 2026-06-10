package dugsolutions.leaf.v30.di

import dugsolutions.leaf.v30.cards.di.cardsModule
import dugsolutions.leaf.v30.grove.di.groveModule
import dugsolutions.leaf.v30.player.di.playerModule
import dugsolutions.leaf.v30.wisp.di.wispModule
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module = module {

    single { Dispatchers.Main }
    single { Dispatchers.IO }
}

val appModules = listOf(
    cardsModule,
    wispModule,
    groveModule,
    playerModule
)
