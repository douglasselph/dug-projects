package dugsolutions.leaf.v35.effect.di

import dugsolutions.leaf.v35.effect.GameEffectConverter
import org.koin.dsl.module

val effectModule = module {
    single { GameEffectConverter() }
}