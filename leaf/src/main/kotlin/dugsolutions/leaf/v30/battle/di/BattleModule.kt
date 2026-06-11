package dugsolutions.leaf.v30.battle.di

import dugsolutions.leaf.v30.battle.Battle
import dugsolutions.leaf.v30.battle.BattleEvaluator
import dugsolutions.leaf.v30.battle.PlayerGridOrder
import org.koin.core.module.Module
import org.koin.dsl.module

val battleModule: Module = module {

    single { PlayerGridOrder(get()) }
    single { BattleEvaluator() }
    single { Battle(get(), get(), get()) }
}
