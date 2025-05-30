package dugsolutions.leaf.main

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.GameCards
import dugsolutions.leaf.cards.GetCards
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.simulator.GameEvent
import dugsolutions.leaf.simulator.GameSimulator
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.game.Game
import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketConfig
import dugsolutions.leaf.grove.scenario.ScenarioBasicConfig
import dugsolutions.leaf.player.decisions.DecisionDrawCount
import dugsolutions.leaf.tool.Randomizer
import dugsolutions.leaf.tool.RandomizerDefault
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random

class MainController(
    private val gameSimulator: GameSimulator,
    private val game: Game,
    private val cardManager: CardManager,
    private val dieFactory: DieFactoryRandom,
    private val grove: Grove,
    private val scenarioBasicConfig: ScenarioBasicConfig,
    private val getCards: GetCards,
    private val randomizer: Randomizer,
    private val runGame: RunGame,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val domain = MainDomain()

    // Create a coroutine scope using the provided dispatcher
    private val scope = CoroutineScope(dispatcher)

    // Expose the domain model for UI observation
    fun getDomain(): MainDomain = domain

    fun run() {
        val numPlayers = 2
        (randomizer as RandomizerDefault).seed = 24
        grove.setup(scenarioBasicConfig(numPlayers))
        game.setup(
            Game.Config(
                numPlayers = 2,
                dieFactory = dieFactory,
                setup = { index, player ->
                    seedlings()
                },
            )
        )
        game.players[0].decisionDirector.drawCountDecision = object : DecisionDrawCount {
            override fun invoke(): Int {
                return 2
            }
        }
        scope.launch {
            runGame().collect { gameEvent ->
                // You can optionally process game events here if needed
                println("Game event: $gameEvent")

                when (gameEvent) {
                    is GameEvent.Started -> domain.addSimulationOutput("Game started")
                    is GameEvent.TurnProgress -> domain.addSimulationOutput(
                        "Turn ${gameEvent.playersScoreData.turn}: ${gameEvent.phase}"
                    )

                    is GameEvent.Completed -> domain.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> TODO()
                }
            }
        }
    }

    private fun seedlings(): GameCards {
        return getCards(FlourishType.SEEDLING).take(4)
    }

}
