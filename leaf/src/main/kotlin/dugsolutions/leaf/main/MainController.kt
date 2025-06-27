package dugsolutions.leaf.main

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.chronicle.report.WriteGameResults
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.GameEvent
import dugsolutions.leaf.main.domain.ItemInfo
import dugsolutions.leaf.main.domain.MainActionDomain
import dugsolutions.leaf.main.domain.MainGameDomain
import dugsolutions.leaf.main.domain.MainOutputDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.MainActionManager
import dugsolutions.leaf.main.gather.MainGameManager
import dugsolutions.leaf.main.gather.MainOutputManager
import dugsolutions.leaf.main.local.CardOperations
import dugsolutions.leaf.main.local.MainActionHandler
import dugsolutions.leaf.main.local.MainDecisions
import dugsolutions.leaf.player.effect.NutrientReward
import dugsolutions.leaf.random.Randomizer
import dugsolutions.leaf.random.RandomizerDefault
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainController(
    private val game: Game,
    private val grove: Grove,
    private val scenarioBasicConfig: ScenarioBasicConfig,
    private val cardOperations: CardOperations,
    private val randomizer: Randomizer,
    private val runGame: RunGame,
    private val mainGameManager: MainGameManager,
    private val mainOutputManager: MainOutputManager,
    private val mainDecisions: MainDecisions,
    private val mainActionManager: MainActionManager,
    private val mainActionHandler: MainActionHandler,
    private val writeGameResults: WriteGameResults,
    private val nutrientReward: NutrientReward,
    private val chronicle: GameChronicle,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val SAVE_DIR = "live"
        private const val SAVE_NAME = "run"
        private const val DEFAULT_SEED = 24L
    }

    private val scope = CoroutineScope(dispatcher)
    private val seed: Long?
        get() = (randomizer as RandomizerDefault).seed

    init {
        chronicle.hasNewEntry = {
            reportNewEntries()
        }
        setup()
    }

    // region public

    val gameState: StateFlow<MainGameDomain> = mainGameManager.state
    val outputState: StateFlow<MainOutputDomain> = mainOutputManager.state
    val actionState: StateFlow<MainActionDomain> = mainActionManager.state

    // region actions

    fun onActionPressed(actionButton: ActionButton) {
        when (actionButton) {
            ActionButton.RUN -> onRunPressed()
            ActionButton.NEXT -> onNextButtonPressed()
            ActionButton.DONE -> onDoneButtonPressed()
            ActionButton.NONE -> {}
        }
    }

    fun onStepEnabledToggled(value: Boolean) {
        runGame.stepMode = value
        mainGameManager.setStepMode(value)
    }

    fun onAskTrashToggled(value: Boolean) {
        mainDecisions.setAskTrash(value)
    }

    private fun onRunPressed() {
        startGame()
        mainActionHandler.clearAction()
    }

    private fun onNextButtonPressed() {
        scope.launch {
            runGame.continueToNextStep()
            mainActionHandler.clearAction()
        }
    }

    private fun onDoneButtonPressed() {
        scope.launch {
            mainDecisions.onPlayerSelectionComplete()
            mainActionHandler.clearAction()
        }
    }


    // endregion actions

    // region DrawCount

    fun onDrawCountChosen(playerInfo: PlayerInfo, value: Int) {
        val player = game.players.find { it.name == playerInfo.name }
        require(player != null)
        mainDecisions.onDrawCountChosen(player, value)
    }

    // endregion DrawCount

    // region GroveSelect

    fun onGroveItemSelected(item: ItemInfo) {
        when (item) {
            is ItemInfo.Card -> {
                mainDecisions.onGroveCardSelected(item.value)
            }

            is ItemInfo.Die -> {
                mainDecisions.onGroveDieSelected(item.value)
            }
        }
    }

    // endregion GroveSelect

    // region PlayerSelect

    fun onHandCardSelected(player: PlayerInfo, card: CardInfo) {
        mainGameManager.setHandCardSelected(player, card)
    }

    fun onFloralCardSelected(player: PlayerInfo, card: CardInfo) {
        mainGameManager.setFloralCardSelected(player, card)
    }

    fun onDieSelected(player: PlayerInfo, die: DieInfo) {
        mainGameManager.setDieSelected(player, die)
    }

    fun onNutrientsClicked(playerInfo: PlayerInfo) {
        val player = game.players.find { it.name == playerInfo.name } ?: return
        nutrientReward(player)
        mainGameManager.resetData()
        mainDecisions.reapplyDecisionId()
    }

    fun onDecidingToggled(playerInfo: PlayerInfo) {
        val player = game.players.find { it.name == playerInfo.name } ?: return
        mainDecisions.onDecisionToggle(player)
        mainGameManager.resetData()
    }

    // endregion PlayerSelect

    // region BooleanInstruction

    fun onBooleanInstructionResponse(response: Boolean) {
        scope.launch {
            mainDecisions.onCardSelectedForEffect(response)
        }
    }

    // endregion BooleanInstruction

    // endregion public

    private fun setup() {
        (randomizer as RandomizerDefault).seed = DEFAULT_SEED
        cardOperations.setup()
        grove.setup(scenarioBasicConfig(numPlayers = 2))
        game.setup(
            Game.Config(
                numPlayers = 2,
                setup = { index, player ->
                    if (index == 0) {
                        mainDecisions.setup(player)
                    }
                    player.setupInitialDeck(seedlings())
                },
            )
        )
        mainGameManager.initialize()
        mainActionManager.initialize()
        mainActionHandler.setActionActive(ActionButton.RUN)
    }

    private fun seedlings(): GameCards {
        return cardOperations.getGameCards(FlourishType.SEEDLING).take(4)
    }

    private fun reportNewEntries() {
        chronicle.getNewEntries().forEach { entry ->
            mainOutputManager.addSimulationOutput(entry.toString())
        }
    }

    private fun startGame() {
        scope.launch {
            runGame().collect { gameEvent ->
                mainGameManager.resetData()
                when (gameEvent) {
                    is GameEvent.Started -> mainOutputManager.addSimulationOutput("Game started. Seed=$seed")
                    is GameEvent.TurnComplete -> mainOutputManager.addSimulationOutput("${gameEvent.phase} Turn ${gameEvent.playersScoreData.turn} Complete")
                    is GameEvent.Completed -> mainOutputManager.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> {
                        mainActionHandler.setActionActive(ActionButton.NEXT)
                    }
                }
                mainGameManager.clearGroveCardHighlights()
                writeGameResults.update(SAVE_DIR, SAVE_NAME)
            }
            writeGameResults.finish(SAVE_DIR, SAVE_NAME)
        }
    }


}
