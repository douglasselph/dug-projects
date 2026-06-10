package dugsolutions.leaf.v30.game.di

import dugsolutions.leaf.v30.game.Game
import org.koin.core.module.Module
import org.koin.dsl.module

val gameModule: Module = module {

    single { Game(get(), get()) }
}
