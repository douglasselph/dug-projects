package dugsolutions.leaf.player.di

import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.FloralBonusCount
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.player.decisions.local.BestCardEvaluator
import dugsolutions.leaf.player.decisions.local.CanPurchaseCard
import dugsolutions.leaf.player.decisions.local.CanPurchaseCards
import dugsolutions.leaf.player.decisions.local.EffectBattleScore
import dugsolutions.leaf.player.effect.CanProcessMatchEffect
import dugsolutions.leaf.player.effect.FlowerCardMatchValue
import dugsolutions.leaf.player.effect.HasDieValue
import dugsolutions.leaf.player.effect.HasFlourishType
import dugsolutions.leaf.game.turn.handle.HandleAdorn
import dugsolutions.leaf.player.effect.ShouldProcessMatchEffect
import org.koin.core.module.Module
import org.koin.dsl.module

val playerModule: Module = module {

    single { CardEffectBattleScoreFactory(get(), get()) }
    single { EffectBattleScore() }
    single { AcquireDieEvaluator() }
    single { AcquireCardEvaluator(get()) }
    single { BestCardEvaluator() }
    single { CanPurchaseCard() }
    single { CanPurchaseCards(get()) }
    single { CanProcessMatchEffect(get(), get()) }
    single { ShouldProcessMatchEffect(get()) }
    single { HasDieValue() }
    single { HasFlourishType(get()) }
    single { FloralBonusCount() }
    single { FlowerCardMatchValue(get()) }
    single { HandleAdorn(get(), get()) }

    factory { FloralArray(get(), get()) }
    factory { StackManager(get(), get()) }
    factory { DecisionDirector(get(), get(), get(), get(), get()) }


    factory {
        DeckManager(
            supply = get(),
            hand = get(),
            compost = get(),
            dieFactory = get()
        )
    }

    single {
        PlayerFactory(
            cardManager = get(),
            deckManager = { get() },
            floralArray = { get() },
            floralBonusCount = get(),
            decisionDirector = { get() },
            costScore = get(),
            dieFactory = get()
        )
    }

}
