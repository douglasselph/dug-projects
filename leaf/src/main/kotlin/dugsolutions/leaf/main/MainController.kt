package dugsolutions.leaf.main

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.GameEvent
import dugsolutions.leaf.main.domain.ItemInfo
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.main.local.CardOperations
import dugsolutions.leaf.main.local.MainDecisions
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
    private val mainDomainManager: MainDomainManager,
    private val mainDecisions: MainDecisions,
    private val chronicle: GameChronicle,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher)

    init {
        chronicle.hasNewEntry = {
            reportNewEntries()
        }
        setup()
    }

    // region public

    val state: StateFlow<MainDomain> = mainDomainManager.state

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
        mainDomainManager.setActionButton(ActionButton.NONE)
        scope.launch {
            runGame().collect { gameEvent ->
                mainDomainManager.updateData()
                when (gameEvent) {
                    is GameEvent.Started -> mainDomainManager.addSimulationOutput("Game started")
                    is GameEvent.TurnComplete -> mainDomainManager.addSimulationOutput("${gameEvent.phase} Turn ${gameEvent.playersScoreData.turn} Complete")
                    is GameEvent.Completed -> mainDomainManager.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> {
                        mainDomainManager.setActionButton(ActionButton.NEXT)
                    }
                }
                mainDomainManager.clearGroveCardHighlights()
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

    fun onStepEnabledToggled(value: Boolean) {
        runGame.stepMode = value
        mainDomainManager.setStepMode(value)
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
        mainDomainManager.setHandCardSelected(player, card)
    }

    fun onFloralCardSelected(player: PlayerInfo, card: CardInfo) {
        mainDomainManager.setFloralCardSelected(player, card)
    }

    fun onDieSelected(player: PlayerInfo, die: DieInfo) {
        mainDomainManager.setDieSelected(player, die)
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
        mainDomainManager.initialize()
        mainDomainManager.setActionButton(ActionButton.RUN)
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
