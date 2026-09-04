package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.round.RoundCoordinator

data class GameRunResult(
    val roundsCompleted: Int
)

/** Runs one fresh Game to completion through its round coordinator. */
class GameRunner(
    private val roundCoordinator: RoundCoordinator
) {

    fun run(game: Game): GameRunResult {
        game.start()

        var roundsCompleted = 0
        while (!game.roundDeck.isEmpty) {
            checkNotNull(roundCoordinator.executeNext(game)) {
                "RoundDeck reported cards remaining but revealed no card"
            }
            roundsCompleted++
        }

        game.complete()
        game.chronicle.record(
            Moment.Marker(
                "GAME_COMPLETED rounds=$roundsCompleted"
            )
        )

        return GameRunResult(
            roundsCompleted = roundsCompleted
        )
    }
}
