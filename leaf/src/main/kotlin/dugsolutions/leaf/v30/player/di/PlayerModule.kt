package dugsolutions.leaf.v30.player.di

import org.koin.core.module.Module
import org.koin.dsl.module

val playerModule: Module = module {

    single { PlayerFactory() }
}
