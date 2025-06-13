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
import dugsolutions.leaf.main.domain.MainGameDomain
import dugsolutions.leaf.main.domain.MainOutputDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.MainGameManager
import dugsolutions.leaf.main.gather.MainOutputManager
import dugsolutions.leaf.main.local.CardOperations
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
    private val writeGameResults: WriteGameResults,
    private val nutrientReward: NutrientReward,
    private val chronicle: GameChronicle,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val SAVE_DIR = "live"
        private const val SAVE_NAME = "run"
    }

    private val scope = CoroutineScope(dispatcher)

    init {
        chronicle.hasNewEntry = {
            reportNewEntries()
        }
        setup()
    }

    // region public

    val gameState: StateFlow<MainGameDomain> = mainGameManager.state
    val outputState: StateFlow<MainOutputDomain> = mainOutputManager.state

    // region actions

    fun onActionPressed(actionButton: ActionButton) {
        when (actionButton) {
            ActionButton.RUN -> onRunPressed()
            ActionButton.NEXT -> onNextButtonPressed()
            ActionButton.DONE -> onDoneButtonPressed()
            ActionButton.NONE -> {}
        }
    }

    private fun onRunPressed() {
        mainGameManager.setActionButton(ActionButton.NONE)
        scope.launch {
            runGame().collect { gameEvent ->
                mainGameManager.resetData()
                when (gameEvent) {
                    is GameEvent.Started -> mainOutputManager.addSimulationOutput("Game started")
                    is GameEvent.TurnComplete -> mainOutputManager.addSimulationOutput("${gameEvent.phase} Turn ${gameEvent.playersScoreData.turn} Complete")
                    is GameEvent.Completed -> mainOutputManager.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> {
                        mainGameManager.setActionButton(ActionButton.NEXT)
                    }
                }
                mainGameManager.clearGroveCardHighlights()
                writeGameResults.update(SAVE_DIR, SAVE_NAME)
            }
            writeGameResults.finish(SAVE_DIR, SAVE_NAME)
        }
    }

    private fun onNextButtonPressed() {
        scope.launch {
            runGame.continueToNextStep()
            mainGameManager.setActionButton(ActionButton.NONE)
        }
    }

    private fun onDoneButtonPressed() {
        scope.launch {
            mainDecisions.onPlayerSelectionComplete()
        }
    }

    fun onStepEnabledToggled(value: Boolean) {
        runGame.stepMode = value
        mainGameManager.setStepMode(value)
    }

    fun onAskTrashToggled(value: Boolean) {
        mainDecisions.setAskTrash(value)
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

    // TODO: Unit test
    fun onNutrientsClicked(playerInfo: PlayerInfo) {
        val player = game.players.find { it.name == playerInfo.name } ?: return
        nutrientReward(player)
        mainGameManager.resetData()
        mainDecisions.reapplyDecisionId()
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
        (randomizer as RandomizerDefault).seed = 24
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
        mainGameManager.setActionButton(ActionButton.RUN)
    }

    private fun seedlings(): GameCards {
        return cardOperations.getGameCards(FlourishType.SEEDLING).take(4)
    }

    private fun reportNewEntries() {
        chronicle.getNewEntries().forEach { entry ->
            mainOutputManager.addSimulationOutput(entry.toString())
        }
    }

}
