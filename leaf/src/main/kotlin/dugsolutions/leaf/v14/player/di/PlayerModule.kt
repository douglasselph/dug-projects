package dugsolutions.leaf.v14.player.di

import dugsolutions.leaf.v14.player.components.DeckManager
import dugsolutions.leaf.v14.player.components.FloralArray
import dugsolutions.leaf.v14.player.effect.FloralBonusCount
import dugsolutions.leaf.v14.player.components.StackManager
import dugsolutions.leaf.v14.player.decisions.DecisionDirector
import dugsolutions.leaf.v14.player.decisions.local.AcquireCardEvaluator
import dugsolutions.leaf.v14.player.decisions.local.AcquireDieEvaluator
import dugsolutions.leaf.v14.player.decisions.local.BestCardEvaluator
import dugsolutions.leaf.v14.player.decisions.local.CanPurchaseCard
import dugsolutions.leaf.v14.player.decisions.local.CanPurchaseCards
import dugsolutions.leaf.v14.player.decisions.local.EffectBattleScore
import dugsolutions.leaf.v14.player.effect.CanProcessMatchEffect
import dugsolutions.leaf.v14.player.effect.FlowerCardMatchValue
import dugsolutions.leaf.v14.player.effect.HasDieValue
import dugsolutions.leaf.v14.player.effect.HasFlourishType
import dugsolutions.leaf.v14.game.turn.handle.HandleAdorn
import dugsolutions.leaf.v14.player.components.DrawNewHand
import dugsolutions.leaf.v14.player.decisions.local.ShouldAskTrashEffect
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionMonitor
import dugsolutions.leaf.v14.player.decisions.local.monitor.DecisionMonitorReport
import dugsolutions.leaf.v14.player.effect.NutrientReward
import dugsolutions.leaf.v14.player.effect.ShouldProcessMatchEffect
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
    single { FlowerCardMatchValue(get(), get()) }
    single { HandleAdorn(get(), get()) }
    single { NutrientReward(get(), get()) }
    single { DrawNewHand() }
    single { ShouldAskTrashEffect() }
    single { DecisionMonitor() }
    single { DecisionMonitorReport(get()) }

    factory { FloralArray(get(), get()) }
    factory { StackManager(get(), get()) }
    factory { DecisionDirector(get(), get(), get(), get(), get()) }


    factory {
        DeckManager(
            supply = get(),
            hand = get(),
            discardPatch = get(),
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
