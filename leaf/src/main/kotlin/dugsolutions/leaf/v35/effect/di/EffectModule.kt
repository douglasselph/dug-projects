package dugsolutions.leaf.v35.effect.di

import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import org.koin.dsl.module

val effectModule = module {
    single { GameEffectConverter() }

    /* Stateless: all mutable game context arrives in GameEffectRequest. */
    single<GameEffectExecutor> { DefaultGameEffectExecutor() }
}
