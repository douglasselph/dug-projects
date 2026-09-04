package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.scoring.FinalScorer
import dugsolutions.leaf.v35.game.scoring.FinalScoringResult

data class GameRunResult(
    val roundsCompleted: Int,
    val finalScoring: FinalScoringResult
)

/** Runs one fresh Game to completion through its round coordinator. */
class GameRunner(
    private val roundCoordinator: RoundCoordinator,
    private val finalScorer: FinalScorer = FinalScorer()
) {

    fun run(game: Game): GameRunResult {
        game.start()

        var roundsCompleted = 0
        while (!game.roundDeck.isEmpty) {
            stateNotNull(roundCoordinator.executeNext(game)) {
                "RoundDeck reported cards remaining but revealed no card"
            }
            roundsCompleted++
        }

        val finalScoring = finalScorer.score(game)

        finalScoring.scores.forEach { score ->
            game.chronicle.record(
                Moment.Marker(
                    "FINAL_SCORE player=${score.playerId.value} " +
                        "existing=${score.existingVp} plants=${score.plantVp} " +
                        "wisps=${score.unplayedWispVp} total=${score.totalVp} " +
                        "plantCount=${score.graftedPlantCount}"
                )
            )
        }

        game.chronicle.record(
            Moment.Marker(
                "FINAL_WINNERS players=" +
                    finalScoring.winnerIds.joinToString(",") { it.value.toString() }
            )
        )

        game.complete()
        game.chronicle.record(
            Moment.Marker(
                "GAME_COMPLETED rounds=$roundsCompleted"
            )
        )

        return GameRunResult(
            roundsCompleted = roundsCompleted,
            finalScoring = finalScoring
        )
    }
}
