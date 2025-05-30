package dugsolutions.leaf.main

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionBestCardPurchase
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import dugsolutions.leaf.player.decisions.ui.DecisionBestCardPurchaseSuspend
import dugsolutions.leaf.player.decisions.ui.DecisionDrawCountSuspend
import dugsolutions.leaf.main.domain.GameEvent
import dugsolutions.leaf.tool.Randomizer
import dugsolutions.leaf.tool.RandomizerDefault
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainController(
    private val game: Game,
    private val dieFactory: DieFactoryRandom,
    private val grove: Grove,
    private val scenarioBasicConfig: ScenarioBasicConfig,
    private val cardOperations: CardOperations,
    private val randomizer: Randomizer,
    private val runGame: RunGame,
    private val mainDomainManager: MainDomainManager,
    private val chronicle: GameChronicle,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher)

    private var decisionDrawCountSuspend: DecisionDrawCountSuspend = DecisionDrawCountSuspend()
    private var decisionBestCardPurchaseSuspend: DecisionBestCardPurchaseSuspend = DecisionBestCardPurchaseSuspend()

    init {
        mainDomainManager.setShowRunButton(true)
        setup()
    }

    // region public

    val state: StateFlow<MainDomain> = mainDomainManager.state

    fun onRunPressed() {
        mainDomainManager.clearShowRunButton()
        update()
        scope.launch {
            runGame().collect { gameEvent ->
                update()
                when (gameEvent) {
                    is GameEvent.Started -> mainDomainManager.addSimulationOutput("Game started")
                    is GameEvent.TurnProgress -> mainDomainManager.addSimulationOutput("Turn ${gameEvent.playersScoreData.turn}: ${gameEvent.phase}")
                    is GameEvent.Completed -> mainDomainManager.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> {
                        mainDomainManager.setShowNextButton(true)
                    }
                }
            }
        }
    }

    fun onDrawCountChosen(value: Int) {
        decisionDrawCountSuspend.provide(value)
        mainDomainManager.clearShowDrawCount()
        update()
    }

    fun onGroveCardSelected(cardInfo: CardInfo) {
        cardOperations.getCard(cardInfo)?.let { card ->
            decisionBestCardPurchaseSuspend.provide(card)
            mainDomainManager.clearGroveCardHighlights()
        }
    }

    // endregion public

    private fun update() {
        reportNewEntries()
        mainDomainManager.update()
    }

    private fun reportNewEntries() {
        chronicle.getNewEntries().forEach { entry ->
            mainDomainManager.addSimulationOutput(entry.toString())
        }
    }

    private fun setup() {
        cardOperations.setup()
        val numPlayers = 2
        (randomizer as RandomizerDefault).seed = 24
        grove.setup(scenarioBasicConfig(numPlayers))
        game.setup(
            Game.Config(
                numPlayers = 2,
                dieFactory = dieFactory,
                setup = { index, player ->
                    if (index == 0) {
                        player.decisionDirector.drawCountDecision = createDecisionDrawCountSuspend(player)
                        player.decisionDirector.bestCardPurchase = createDecisionBestCardPurchaseSuspend(player)
                    }
                    player.setupInitialDeck(seedlings())
                },
            )
        )
        update()
    }

    private fun seedlings(): GameCards {
        return cardOperations.getGameCards(FlourishType.SEEDLING).take(4)
    }

    fun onStepEnabledToggled(value: Boolean) {
        runGame.stepMode = value
        mainDomainManager.setStepMode(value)
    }

    fun onNextButtonPressed() {
        scope.launch {
            runGame.continueToNextStep()
            mainDomainManager.clearShowNextButton()
        }
    }

    private fun createDecisionDrawCountSuspend(player: Player): DecisionDrawCount {
        val value = DecisionDrawCountSuspend()
        value.onDrawCountRequest = {
            update()
            mainDomainManager.setShowDrawCount(player, true)
            decisionDrawCountSuspend = value
        }
        return value
    }

    private fun createDecisionBestCardPurchaseSuspend(player: Player): DecisionBestCardPurchase {
        val value = DecisionBestCardPurchaseSuspend()
        value.onBestCardPurchase = { possibleCards ->
            update()
            mainDomainManager.setHighlightGroveCardsForSelection(possibleCards, player)
            decisionBestCardPurchaseSuspend = value
        }
        return value
    }


}
