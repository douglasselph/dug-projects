package dugsolutions.leaf.v14.game.di

import dugsolutions.leaf.v14.game.Game
import dugsolutions.leaf.v14.game.RunGame
import dugsolutions.leaf.v14.game.acquire.AcquireItem
import dugsolutions.leaf.v14.game.acquire.HandleGroveAcquisition
import dugsolutions.leaf.v14.game.acquire.ManageAcquiredFloralTypes
import dugsolutions.leaf.v14.game.acquire.cost.ApplyCost
import dugsolutions.leaf.v14.game.acquire.evaluator.CombinationGenerator
import dugsolutions.leaf.v14.game.acquire.evaluator.PossibleCards
import dugsolutions.leaf.v14.game.acquire.evaluator.PossibleDice
import dugsolutions.leaf.v14.game.battle.BattlePhaseTransition
import dugsolutions.leaf.v14.game.battle.BestFlowerCards
import dugsolutions.leaf.v14.game.battle.HandleAbsorbDamage
import dugsolutions.leaf.v14.game.battle.HandleDeliverDamage
import dugsolutions.leaf.v14.game.battle.MatchingBloomCard
import dugsolutions.leaf.v14.game.domain.GameTime
import dugsolutions.leaf.v14.game.turn.PlayerOrder
import dugsolutions.leaf.v14.game.turn.PlayerRound
import dugsolutions.leaf.v14.game.turn.PlayerTurn
import dugsolutions.leaf.v14.game.turn.effect.EffectCardToRetain
import dugsolutions.leaf.v14.game.turn.effect.EffectDieAdjust
import dugsolutions.leaf.v14.game.turn.effect.EffectDieReroll
import dugsolutions.leaf.v14.game.turn.effect.EffectDieRerollAny
import dugsolutions.leaf.v14.game.turn.effect.EffectDieToMax
import dugsolutions.leaf.v14.game.turn.effect.EffectDieToRetain
import dugsolutions.leaf.v14.game.turn.effect.EffectDiscard
import dugsolutions.leaf.v14.game.turn.effect.EffectDraw
import dugsolutions.leaf.v14.game.turn.effect.EffectDrawCard
import dugsolutions.leaf.v14.game.turn.effect.EffectDrawDie
import dugsolutions.leaf.v14.game.turn.effect.EffectGainD20
import dugsolutions.leaf.v14.game.turn.effect.EffectReplayVine
import dugsolutions.leaf.v14.game.turn.effect.EffectReuse
import dugsolutions.leaf.v14.game.turn.effect.EffectReuseCard
import dugsolutions.leaf.v14.game.turn.effect.EffectReuseDie
import dugsolutions.leaf.v14.game.turn.effect.EffectUseOpponentCard
import dugsolutions.leaf.v14.game.turn.effect.EffectUseOpponentDie
import dugsolutions.leaf.v14.game.turn.handle.HandleAdorn
import dugsolutions.leaf.v14.game.turn.handle.HandleCard
import dugsolutions.leaf.v14.game.turn.handle.HandleCardEffect
import dugsolutions.leaf.v14.game.turn.handle.HandleCleanup
import dugsolutions.leaf.v14.game.turn.handle.HandleCompostRecovery
import dugsolutions.leaf.v14.game.turn.handle.HandleDieUpgrade
import dugsolutions.leaf.v14.game.turn.handle.HandleDrawHand
import dugsolutions.leaf.v14.game.turn.handle.HandleGetTarget
import dugsolutions.leaf.v14.game.turn.handle.HandleRetained
import dugsolutions.leaf.v14.game.turn.handle.HandleReused
import dugsolutions.leaf.v14.game.turn.local.CardIsFree
import dugsolutions.leaf.v14.game.turn.local.EvaluateSimpleCost
import dugsolutions.leaf.v14.game.turn.select.SelectAllDice
import dugsolutions.leaf.v14.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.v14.game.turn.select.SelectDiceNotActivatingMatches
import dugsolutions.leaf.v14.game.turn.select.SelectDieAnyToReroll
import dugsolutions.leaf.v14.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.v14.game.turn.select.SelectDieToMax
import dugsolutions.leaf.v14.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.v14.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.v14.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.v14.game.turn.select.SelectPossibleDice
import org.koin.dsl.module

val gameModule: Module = module {

    single { GameTime() }
    single { PlayerOrder(get()) }
    single { AcquireItem(get(), get(), get(), get(), get(), get(), get()) }
    single { PossibleDice(get(), get()) }
    single { PossibleCards(get()) }
    single { EvaluateSimpleCost(get()) }
    single { CombinationGenerator() }
    single { ApplyCost() }
    single { CardIsFree() }

    single { SelectCardToRetain(get()) }
    single { SelectDieToReroll() }
    single { SelectDieAnyToReroll(get()) }
    single { SelectDieToRetain(get()) }
    single { SelectDieToMax() }
    single { SelectDieToAdjust() }
    single { SelectPossibleCards(get(), get()) }
    single { SelectPossibleDice(get(), get()) }
    single { SelectAllDice(get(), get()) }
    single { SelectDiceNotActivatingMatches(get()) }

    single { EffectCardToRetain(get(), get()) }
    single { EffectDieAdjust(get(), get()) }
    single { EffectDieToMax(get(), get()) }
    single { EffectDieReroll(get(), get(), get()) }
    single { EffectDieRerollAny(get(), get()) }
    single { EffectDieToRetain(get(), get()) }
    single { EffectDiscard(get(), get()) }
    single { EffectDrawCard(get(), get()) }
    single { EffectDrawDie(get()) }
    single { EffectDraw(get(), get()) }
    single { EffectGainD20(get(), get(), get()) }
    single { EffectReuseCard(get(), get()) }
    single { EffectReuseDie(get()) }
    single { EffectReuse(get(), get()) }
    single { EffectReplayVine(get()) }
    single { EffectUseOpponentDie(get()) }
    single { EffectUseOpponentCard(get(), get()) }

    single { HandleAdorn(get(), get()) }
    single { HandleCompostRecovery(get()) }
    single { HandleCleanup(get(), get(), get(), get(), get()) }
    single { HandleDeliverDamage(get(), get()) }
    single { HandleDrawHand(get(), get()) }
    single { HandleAbsorbDamage(get()) }
    single { HandleGroveAcquisition(get(), get(), get(), get()) }
    single { HandleGetTarget() }
    single { HandleDieUpgrade(get(), get(), get(), get(), get()) }
    single { HandleRetained() }
    single { HandleReused() }
    single {
        HandleCardEffect(
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get()
        )
    }
    single { HandleCard(get(), get(), get(), get(), get(), get()) }
    single { PlayerRound(get(), get()) }

    single {
        PlayerTurn(
            get(), get(), get(),
            get(), get(), get(), get()
        )
    }

    single { Game(get(), get(), get(), get(), get(), get(), get()) }
    single { RunGame(get(), get(), get()) }

    single { ManageAcquiredFloralTypes() }
    single { MatchingBloomCard(get()) }
    single { BestFlowerCards(get()) }
    single { BattlePhaseTransition(get(), get(), get(), get()) }

} 
