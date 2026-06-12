package dugsolutions.leaf.v30.game.di

import dugsolutions.leaf.v30.game.Game
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutorBattle
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutorCultivation
import dugsolutions.leaf.v30.game.effect.RoundActionExecutor
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import org.koin.core.module.Module
import org.koin.dsl.module

val gameModule: Module = module {

    single { RoundActionExecutor() }
    single { WispCardEffectExecutor() }
    single { GameCardEffectExecutorCultivation(get(), get()) }
    single { GameCardEffectExecutorBattle(get(), get()) }
    single { Game(get(), get(), get(), get(), get(), get()) }

}
