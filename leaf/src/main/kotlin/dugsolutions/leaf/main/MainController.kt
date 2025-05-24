package dugsolutions.leaf.main

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.simulator.GameEvent
import dugsolutions.leaf.simulator.GameSimulator
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.game.Game
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainController(
    private val gameSimulator: GameSimulator,
    private val game: Game,
    private val cardManager: CardManager,
    private val dieFactory: DieFactoryRandom,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val domain = MainDomain()
    private val numPlayers: Int
        get() = domain.state.numPlayers

    private val numGames: Int
        get() = domain.state.numGames

    // Create a coroutine scope using the provided dispatcher
    private val scope = CoroutineScope(dispatcher)

    // Expose the domain model for UI observation
    fun getDomain(): MainDomain = domain

    fun run() {
        println("Setting up game with ${domain.state.numPlayers} players")

        val seedlings = cardManager.getGameCardsByType(FlourishType.SEEDLING)

        // Launch coroutine to run simulation
        scope.launch {
            gameSimulator.runGamesSequentially(
                numGames,
                setup = {
                    game.setup(
                        Game.Config(
                            numPlayers = 2,
                            dieFactory = dieFactory,
                            setup = { index, player ->
                                player.setupInitialDeck(seedlings.shuffled().take(2))
                            }
                        )
                    )
                }
            ).collect { event ->
                when (event) {
                    is GameEvent.Started -> domain.addSimulationOutput("Game started")
                    is GameEvent.TurnProgress -> domain.addSimulationOutput(
                        "Turn ${event.playersScoreData.turn}: ${event.phase}"
                    )
                    is GameEvent.Completed -> domain.addSimulationOutput("Game completed")
                    GameEvent.WaitForStep -> TODO()
                }
            }
        }
    }
}
