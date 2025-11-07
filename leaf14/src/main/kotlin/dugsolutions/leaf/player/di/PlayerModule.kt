package dugsolutions.leaf.player.di

import dugsolutions.leaf.player.components.ButterflyManager
import dugsolutions.leaf.player.components.CreatureManager
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.InsectManager
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.components.VPManager
import dugsolutions.leaf.player.components.WispManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireCard
import dugsolutions.leaf.player.decisions.local.EvaluateAcquireDie
import dugsolutions.leaf.player.decisions.local.BestCardEvaluator
import dugsolutions.leaf.player.effect.HasDieValue
import dugsolutions.leaf.player.effect.HasFlourishType
import dugsolutions.leaf.player.effect.ShouldProcessMatchEffect
import org.koin.core.module.Module
import org.koin.dsl.module

val playerModule: Module = module {

    single { EvaluateAcquireDie() }
    single { EvaluateAcquireCard(get()) }
    single { BestCardEvaluator() }
    single { ShouldProcessMatchEffect(get()) }
    single { HasDieValue() }
    single { HasFlourishType(get()) }

    factory { StackManager(get(), get()) }
    factory { DecisionDirector(get()) }
    factory { ButterflyManager() }
    factory { CreatureManager(get()) }
    factory { InsectManager() }
    factory { WispManager() }
    factory { VPManager() }

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
            wispManager = { get() },
            vpManager = { get() },
            decisionDirector = { get() },
            costScore = get(),
            dieFactory = get()
        )
    }

}
