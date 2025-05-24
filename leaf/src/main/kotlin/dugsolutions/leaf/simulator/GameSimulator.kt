package dugsolutions.leaf.simulator

import dugsolutions.leaf.game.RunGame
import dugsolutions.leaf.player.domain.PlayersScoreData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GameSimulator(
    private val runGame: RunGame,
) {

    fun runGamesSequentially(numGames: Int, setup: (gameIndex: Int) -> Unit): Flow<GameEvent> =
        flow {
            for (gameIndex in 0 until numGames) {
                // Set up the game for this iteration
                setup(gameIndex)

                // Run the game and emit all events from it
                runGame().collect { event ->
                    // Add game index to the event context
                    val enrichedEvent = when (event) {
                        is GameEvent.Completed -> GameEvent.Completed(event.result)
                        else -> event
                    }
                    emit(enrichedEvent)
                }
            }
        }

    // For running multiple games in p
    // arallel and analyzing results
    suspend fun runGamesInParallel(
        numGames: Int,
        setup: (gameIndex: Int) -> Unit
    ): List<PlayersScoreData> = coroutineScope {
        val deferredResults = List(numGames) { gameIndex ->
            async {
                setup(gameIndex)
                val gameResult = mutableListOf<PlayersScoreData>()
                runGame()
                    .collect { event ->
                        when (event) {
                            is GameEvent.Completed -> gameResult.add(event.result)
                            else -> {} // Ignore other events
                        }
                    }
                gameResult.first() // Each game should produce exactly one result
            }
        }
        deferredResults.awaitAll()
    }

}
