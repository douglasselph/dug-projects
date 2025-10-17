package dugsolutions.leaf.player.di

import dugsolutions.leaf.player.components.ButterflyManager
import dugsolutions.leaf.player.components.CreatureManager
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.InsectManager
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.decisions.local.BestCardEvaluator
import dugsolutions.leaf.player.effect.HasDieValue
import dugsolutions.leaf.player.effect.HasFlourishType
import dugsolutions.leaf.player.effect.ShouldProcessMatchEffect
import org.koin.core.module.Module
import org.koin.dsl.module

val playerModule: Module = module {

    single { AcquireDieEvaluator() }
    single { AcquireCardEvaluator(get()) }
    single { BestCardEvaluator() }
    single { ShouldProcessMatchEffect(get()) }
    single { HasDieValue() }
    single { HasFlourishType(get()) }

    factory { StackManager(get(), get()) }
    factory { DecisionDirector(get(), get(), get()) }
    factory { ButterflyManager() }
    factory { CreatureManager(get()) }
    factory { InsectManager() }

    factory {
        DeckManager(
            supply = get(),
            hand = get(),
            discardPile = get(),
            dieFactory = get()
        )
    }

    single {
        PlayerFactory(
            cardManager = get(),
            deckManager = { get() },
            creatureManager = { get() },
            insectManager = { get() },
            butterflyManager = { get() },
            decisionDirector = { get() },
            costScore = get(),
            dieFactory = get()
        )
    }

}
