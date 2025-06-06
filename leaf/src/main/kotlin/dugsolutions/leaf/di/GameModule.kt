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
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.game.acquire.AcquireItem
import dugsolutions.leaf.game.acquire.HandleGroveAcquisition
import dugsolutions.leaf.game.acquire.ManageAcquiredFloralTypes
import dugsolutions.leaf.game.acquire.cost.ApplyCost
import dugsolutions.leaf.game.acquire.cost.ApplyEffects
import dugsolutions.leaf.game.acquire.credit.CombinationGenerator
import dugsolutions.leaf.game.acquire.credit.EffectToCredits
import dugsolutions.leaf.game.acquire.evaluator.AcquireCardEvaluator
import dugsolutions.leaf.game.acquire.evaluator.AcquireDieEvaluator
import dugsolutions.leaf.game.acquire.evaluator.EvaluateBestDiePurchase
import dugsolutions.leaf.game.acquire.evaluator.EvaluateCardPurchases
import dugsolutions.leaf.game.battle.BattlePhaseTransition
import dugsolutions.leaf.game.battle.BestFlowerCards
import dugsolutions.leaf.game.battle.HandleAbsorbDamage
import dugsolutions.leaf.game.battle.HandleDeliverDamage
import dugsolutions.leaf.game.battle.MatchingBloomCard
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.game.turn.PlayerOrder
import dugsolutions.leaf.game.turn.PlayerRound
import dugsolutions.leaf.game.turn.PlayerTurn
import dugsolutions.leaf.game.turn.handle.HandleCardEffect
import dugsolutions.leaf.game.turn.handle.HandleCleanup
import dugsolutions.leaf.game.turn.handle.HandleDieUpgrade
import dugsolutions.leaf.game.turn.handle.HandleGetTarget
import dugsolutions.leaf.game.turn.handle.HandleLimitedDieUpgrade
import dugsolutions.leaf.game.turn.local.CardIsFree
import dugsolutions.leaf.game.turn.local.EvaluateSimpleCost
import dugsolutions.leaf.game.turn.select.SelectBestDie
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.game.turn.select.SelectDieToMax
import dugsolutions.leaf.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.game.turn.select.SelectPossibleCards
import dugsolutions.leaf.game.turn.select.SelectPossibleDice
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.GameCardsUseCase
import dugsolutions.leaf.grove.domain.GroveStacks
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.main.CardOperations
import dugsolutions.leaf.main.MainController
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo
import dugsolutions.leaf.main.gather.GatherGroveInfo
import dugsolutions.leaf.main.gather.GatherPlayerInfo
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.baseline.DecisionBestCardPurchaseBaseline
import dugsolutions.leaf.player.effect.CanProcessMatchEffect
import dugsolutions.leaf.player.effect.CardEffectProcessor
import dugsolutions.leaf.player.effect.CardEffectsProcessor
import dugsolutions.leaf.player.effect.CardsEffectsProcessor
import dugsolutions.leaf.player.effect.HasDieValue
import dugsolutions.leaf.player.effect.HasFlourishType
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

    single { Dispatchers.Main }
    single { Dispatchers.IO }

    single { CostScore() }
    single { ParseCost() }

    single { CardRegistry(get()) }
    single { CardManager(get()) }
    single { GameCardIDsFactory(get(), get()) }
    single { GameCardsFactory(get(), get()) }
    single { CardOperations(get(), get(), get()) }

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

    single { TransformMomentToEntry(get(), get(), get(), get()) }
    single { GatherCardInfo() }
    single { GatherDiceInfo() }
    single { GatherGroveInfo(get(), get()) }
    single { GatherPlayerInfo(get(), get()) }
    single { MainDomainManager(get(), get(), get(), get()) }

    single {
        MainController(
            get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }

    factory { FloralArray(get(), get()) }
    factory { StackManager(get(), get()) }

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
            floralArray = { get() },
            decisionDirectorFactory = get(),
            costScore = get(),
            chronicle = get()
        )
    }

    single { HasDieValue() }
    single { HasFlourishType(get()) }
    single { ManageAcquiredFloralTypes() }
    single { MatchingBloomCard(get()) }
    single { BestFlowerCards(get()) }
    single { BattlePhaseTransition(get(), get()) }

    single { Game(get(), get(), get(), get(), get(), get()) }
    single { RunGame(get(), get(), get()) }

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
    single { Grove(get(), get()) }
    single { PlayerUnderTest(get()) }

    single {
        GroveStacks(
            cardManager = get(),
            gameCardIDsFactory = get()
        )
    }

    single {
        PlayerTurn(
            get(), get(), get(),
            get(), get(), get()
        )
    }

    single { PlayerRound(get(), get()) }
    single { DieCost() }
    single { HandleDeliverDamage(get(), get()) }
    single { HandleCleanup() }
    single { HandleAbsorbDamage(get()) }
    single { HandleGroveAcquisition(get(), get(), get()) }
    single { HandleGetTarget() }
    single { HandleDieUpgrade(get(), get(), get()) }
    single { HandleLimitedDieUpgrade(get()) }
    single {
        HandleCardEffect(
            get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get()
        )
    }
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

    single { CardsEffectsProcessor(get()) }
    single { CardEffectsProcessor(get(), get()) }
    single { CardEffectProcessor(get()) }
    single { EvaluateBestDiePurchase(get()) }
    single { EvaluateCardPurchases() }
    single { EvaluateSimpleCost(get()) }

    single { DecisionBestCardPurchaseBaseline(get()) }
    single { AcquireCardEvaluator(get()) }
    single { AcquireDieEvaluator(get(), get()) }
    single { AcquireItem(get(), get(), get(), get(), get(), get(), get()) }

    single {
        CanProcessMatchEffect(
            hasDieValue = get(),
            hasFlourishType = get()
        )
    }
    single { PlayerOrder(get()) }
    single { GameTurn() }

    single { ScenarioBasicConfig(get()) }

} 
