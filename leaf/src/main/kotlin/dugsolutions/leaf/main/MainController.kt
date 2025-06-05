package dugsolutions.leaf.main

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.di.factory.DieFactoryRandom
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.GameEvent
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.main.local.CardOperations
import dugsolutions.leaf.main.local.MainDecisions
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
    private val mainDecisions: MainDecisions,
    private val chronicle: GameChronicle,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher)

    init {
        mainDomainManager.setActionButton(ActionButton.RUN)
        chronicle.hasNewEntry = {
            reportNewEntries()
        }
        setup()
    }

    // region public

    val state: StateFlow<MainDomain> = mainDomainManager.state

    fun onActionPressed(actionButton: ActionButton) {
        when(actionButton) {
            ActionButton.RUN -> onRunPressed()
            ActionButton.NEXT -> onNextButtonPressed()
            ActionButton.DONE -> onDoneButtonPressed()
            ActionButton.NONE -> {}
        }
    }
    private fun onRunPressed() {
        mainDomainManager.setActionButton(ActionButton.NONE)
        scope.launch {
            runGame().collect { gameEvent ->
                when (gameEvent) {
                    is GameEvent.Started -> mainDomainManager.addSimulationOutput("Game started")
                    is GameEvent.TurnProgress -> mainDomainManager.addSimulationOutput("Turn ${gameEvent.playersScoreData.turn}: ${gameEvent.phase}")
                    is GameEvent.Completed -> mainDomainManager.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> {
                        mainDomainManager.setActionButton(ActionButton.NEXT)
                    }
                }
            }
        }
    }

    private fun onNextButtonPressed() {
        scope.launch {
            runGame.continueToNextStep()
            mainDomainManager.setActionButton(ActionButton.NONE)
        }
    }

    private fun onDoneButtonPressed() {
        scope.launch {
            mainDecisions.onPlayerSelectionComplete()
        }
    }

    fun onDrawCountChosen(value: Int) {
        mainDecisions.onDrawCountChosen(value)
    }

    fun onGroveCardSelected(cardInfo: CardInfo) {
        mainDecisions.onGroveCardSelected(cardInfo)
    }

    fun onStepEnabledToggled(value: Boolean) {
        runGame.stepMode = value
        mainDomainManager.setStepMode(value)
    }

    fun onHandCardSelected(player: PlayerInfo, card: CardInfo) {
        mainDomainManager.setHandCardSelected(player, card)
    }

    fun onFloralCardSelected(player: PlayerInfo, card: CardInfo) {
        mainDomainManager.setFloralCardSelected(player, card)
    }

    fun onDieSelected(player: PlayerInfo, die: DieInfo) {
        mainDomainManager.setDieSelected(player, die)
    }

    // endregion public

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
                        mainDecisions.setup(player)
                    }
                    player.setupInitialDeck(seedlings())
                },
            )
        )
        mainDomainManager.initialize()
    }

    private fun seedlings(): GameCards {
        return cardOperations.getGameCards(FlourishType.SEEDLING).take(4)
    }

    private fun reportNewEntries() {
        chronicle.getNewEntries().forEach { entry ->
            mainDomainManager.addSimulationOutput(entry.toString())
        }
    }

}
