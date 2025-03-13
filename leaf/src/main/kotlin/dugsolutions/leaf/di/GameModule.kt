package dugsolutions.leaf.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.PlayerUnderTest
import dugsolutions.leaf.chronicle.domain.TestOutputFile
import dugsolutions.leaf.chronicle.domain.TransformMomentToEntry
import dugsolutions.leaf.chronicle.report.GenerateGameSummaries
import dugsolutions.leaf.chronicle.report.GenerateGameSummary
import dugsolutions.leaf.chronicle.report.ReportDamage
import dugsolutions.leaf.chronicle.report.ReportGameAnalysis
import dugsolutions.leaf.chronicle.report.ReportGameBrief
import dugsolutions.leaf.chronicle.report.ReportGameSummaries
import dugsolutions.leaf.chronicle.report.ReportGameSummary
import dugsolutions.leaf.chronicle.report.ReportPlayer
import dugsolutions.leaf.chronicle.report.WriteChronicleResults
import dugsolutions.leaf.chronicle.report.WriteGameResults
import dugsolutions.leaf.chronicle.report.WriteGameSummaries
import dugsolutions.leaf.chronicle.report.WriteToFile
import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.game.purchase.PurchaseItem
import dugsolutions.leaf.game.purchase.cost.ApplyCost
import dugsolutions.leaf.game.purchase.cost.ApplyEffects
import dugsolutions.leaf.game.purchase.credit.CombinationGenerator
import dugsolutions.leaf.game.purchase.credit.EffectToCredits
import dugsolutions.leaf.game.purchase.evaluator.EvaluateBestDiePurchase
import dugsolutions.leaf.game.purchase.evaluator.EvaluateCardPurchases
import dugsolutions.leaf.game.purchase.evaluator.PurchaseCardEvaluator
import dugsolutions.leaf.game.purchase.evaluator.PurchaseDieEvaluator
import dugsolutions.leaf.game.battle.HandleAbsorbDamage
import dugsolutions.leaf.game.battle.HandleBattleEffects
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.game.battle.HandleDeliverDamage
import dugsolutions.leaf.game.turn.handle.HandleDrawEffect
import dugsolutions.leaf.game.turn.handle.HandleGetTarget
import dugsolutions.leaf.game.turn.handle.HandleLocalCardEffect
import dugsolutions.leaf.game.purchase.HandleMarketAcquisition
import dugsolutions.leaf.game.purchase.ManagePurchasedFloralTypes
import dugsolutions.leaf.game.turn.handle.HandleOpponentEffects
import dugsolutions.leaf.game.turn.handle.HandlePassOrPlay
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.PlayerRound
import dugsolutions.leaf.game.turn.PlayerTurn
import dugsolutions.leaf.game.turn.config.PlayerBattlePhaseCheck2D20
import dugsolutions.leaf.game.turn.config.PlayerBattlePhaseCheckBloom
import dugsolutions.leaf.game.turn.config.PlayerReadyForBattlePhase
import dugsolutions.leaf.game.turn.config.PlayerSetupForBattlePhase
import dugsolutions.leaf.game.turn.local.EvaluateSimpleCost
import dugsolutions.leaf.game.turn.local.CardIsFree
import dugsolutions.leaf.game.turn.handle.HandleDieUpgrade
import dugsolutions.leaf.game.turn.handle.HandleLimitedDieUpgrade
import dugsolutions.leaf.game.turn.select.SelectBestDie
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.game.turn.select.SelectDieToMax
import dugsolutions.leaf.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.game.turn.select.SelectPossibleDice
import dugsolutions.leaf.main.MainController
import dugsolutions.leaf.main.info.GatherPlayerInfo
import dugsolutions.leaf.market.Market
import dugsolutions.leaf.market.domain.GameCardsUseCase
import dugsolutions.leaf.market.local.MarketStacks
import dugsolutions.leaf.market.scenario.ScenarioMarketCheap
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionBestCardPurchaseCoreStrategy
import dugsolutions.leaf.player.effect.CardEffectProcessor
import dugsolutions.leaf.player.effect.CardEffectsProcessor
import dugsolutions.leaf.player.effect.HasDieValue
import dugsolutions.leaf.player.effect.HasFlourishType
import dugsolutions.leaf.player.effect.ShouldProcessMatchEffect
import dugsolutions.leaf.simulator.GameSimulator
import dugsolutions.leaf.tool.CardRegistry
import dugsolutions.leaf.tool.ParseCost
import dugsolutions.leaf.tool.Randomizer
import dugsolutions.leaf.tool.RandomizerDefault
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

// A global flag to determine whether to use uniform or random dice
object DieFactoryConfig {
    var useUniformDice = false
}

val gameModule: Module = module {

    single { CostScore() }
    single { ParseCost() }

    single { CardRegistry(get()) }
    single { CardManager(get()) }
    single { GameCardIDsFactory(get(), get()) }
    single { GameCardsFactory(get(), get()) }

    // Common randomizer used by both die factories
    single<Randomizer> { RandomizerDefault() }

    // Provide the DieFactory based on the current configuration
    single<DieFactory> {
        if (DieFactoryConfig.useUniformDice) {
            DieFactoryUniform(get())
        } else {
            DieFactoryRandom(get())
        }
    }

    // Keep factories available for explicit usage if needed
    single { DieFactoryUniform(get()) }
    single { DieFactoryRandom(get()) }

    single { Dispatchers.Main }
    single { Dispatchers.IO }

    single { TransformMomentToEntry(get(), get(), get(), get()) }
    single { PlayerBattlePhaseCheck2D20(get(), get()) }
    single { PlayerBattlePhaseCheckBloom(get()) }
    single { PlayerSetupForBattlePhase(get()) }

    single {
        MainController(
            get(), get(), get(), get(), get(), get()
        )
    }

    single { ScenarioMarketCheap(get(), get()) }

    factory {
        StackManager(
            get(),
            get()
        )
    }

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
            retainedStack = { get() },
            deckManager = { get() },
            decisionDirectorFactory = get(),
            costScore = get(),
            chronicle = get()
        )
    }

    single { HasDieValue() }
    single { HasFlourishType(get()) }
    single { ManagePurchasedFloralTypes() }

    single { Game(get(), get(), get(), get(), get(), get(), get()) }

    single { RunGame(get(), get(), get()) }
    single { GameSimulator(get()) }

    single { GameChronicle(get(), get()) }
    single { WriteToFile() }
    single { TestOutputFile() }

    single { GenerateGameSummary(get(), get()) }
    single { GenerateGameSummaries() }
    single { ReportPlayer() }
    single { ReportDamage() }
    single { ReportGameBrief(get()) }
    single { ReportGameSummary() }
    single { ReportGameSummaries() }
    single { ReportGameAnalysis(get()) }
    single { WriteGameResults(get(), get(), get(), get(), get()) }
    single { WriteChronicleResults(get(), get()) }
    single { WriteGameSummaries(get(), get(), get(), get()) }

    single { DecisionDirectorFactory(get()) }

    single { GameCardsUseCase(get()) }
    single { Market(get(), get()) }
    single { GatherPlayerInfo() }
    single { PlayerUnderTest(get()) }

    single {
        MarketStacks(
            cardManager = get(),
            gameCardIDsFactory = get(),
            dieFactory = get()
        )
    }

    single {
        PlayerTurn(
            get(), get(), get(), get(),
            get(), get(), get(), get()
        )
    }

    single {
        PlayerRound(
            get(), get(),
            get(), get()
        )
    }

    single { DieCost() }
    single { HandleDeliverDamage(get()) }
    single { HandleCleanup() }
    single { HandleDrawEffect(get(), get(), get()) }
    single { HandleAbsorbDamage(get()) }
    single { HandleMarketAcquisition(get(), get(), get()) }
    single { HandleBattleEffects(get(), get(), get()) }
    single { HandlePassOrPlay() }
    single { HandleOpponentEffects(get(), get(), get()) }
    single { HandleGetTarget(get()) }
    single { HandleDieUpgrade(get(), get(), get()) }
    single { HandleLimitedDieUpgrade(get()) }
    single { SelectBestDie(get(), get(), get()) }
    single { SelectCardToRetain(get()) }
    single { SelectDieToReroll() }
    single { SelectDieToRetain(get()) }
    single { SelectDieToMax() }
    single { SelectDieToAdjust() }
    single { SelectPossibleCards(get(), get()) }
    single { SelectPossibleDice(get(), get()) }
    single { CardIsFree() }
    single { EffectToCredits() }
    single { ApplyEffects() }
    single { ApplyCost(get()) }
    single { CombinationGenerator(get()) }
    single { PlayerReadyForBattlePhase(get()) }

    single { CardEffectsProcessor(get(), get()) }
    single { CardEffectProcessor(get()) }
    single { EvaluateBestDiePurchase(get()) }
    single { EvaluateCardPurchases() }
    single { EvaluateSimpleCost(get()) }

    single { DecisionBestCardPurchaseCoreStrategy(get()) }
    single { PurchaseCardEvaluator(get()) }
    single { PurchaseDieEvaluator(get(), get()) }
    single { PurchaseItem(get(), get(), get(), get(), get(), get()) }

    single { HandleLocalCardEffect(get(), get(), get(), get(), get(), get()) }

    single {
        ShouldProcessMatchEffect(
            hasDieValue = get(),
            hasFlourishType = get()
        )
    }
    single { PlayerOrder(get()) }
    single { GameTurn() }
} 
