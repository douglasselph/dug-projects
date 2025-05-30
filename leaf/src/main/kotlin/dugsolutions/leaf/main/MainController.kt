package dugsolutions.leaf.main

import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.GetCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.gather.MainDomainManager
import dugsolutions.leaf.player.decisions.ui.DecisionDrawCountSuspend
import dugsolutions.leaf.simulator.GameEvent
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
    private val getCards: GetCards,
    private val randomizer: Randomizer,
    private val runGame: RunGame,
    private val mainDomainManager: MainDomainManager,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // Create a coroutine scope using the provided dispatcher
    private val scope = CoroutineScope(dispatcher)

    private val decisionDrawCountSuspend = DecisionDrawCountSuspend()

    init {
        decisionDrawCountSuspend.onDrawCountRequest = {
            mainDomainManager.showDrawCount = true
        }
        mainDomainManager.showRunButton = true
    }

    // Expose the state flow
    val state: StateFlow<MainDomain> = mainDomainManager.state

    fun update() {
        mainDomainManager.update()
    }

    fun onDrawCountChosen(value: Int) {
        decisionDrawCountSuspend.provide(value)
        mainDomainManager.showDrawCount = false
    }

    fun onRunPressed() {
        mainDomainManager.showRunButton = false
        val numPlayers = 2
        (randomizer as RandomizerDefault).seed = 24
        grove.setup(scenarioBasicConfig(numPlayers))
        game.setup(
            Game.Config(
                numPlayers = 2,
                dieFactory = dieFactory,
                setup = { index, player ->
                    if (index == 0) {
                        player.decisionDirector.drawCountDecision = decisionDrawCountSuspend
                    }
                    seedlings()
                },
            )
        )
        scope.launch {
            runGame().collect { gameEvent ->
                update()
                when (gameEvent) {
                    is GameEvent.Started -> mainDomainManager.addSimulationOutput("Game started")
                    is GameEvent.TurnProgress -> mainDomainManager.addSimulationOutput("Turn ${gameEvent.playersScoreData.turn}: ${gameEvent.phase}")
                    is GameEvent.Completed -> mainDomainManager.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> {
                        mainDomainManager.showNextButton = true
                    }
                }
            }
        }
    }

    private fun seedlings(): GameCards {
        return getCards(FlourishType.SEEDLING).take(4)
    }

    fun onStepEnabledToggled(value: Boolean) {
        runGame.stepMode = value
    }

    fun onNextButtonPressed() {
        scope.launch {
            runGame.continueToNextStep()
            mainDomainManager.showNextButton = false
        }
    }
}
