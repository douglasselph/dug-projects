package dugsolutions.leaf.game

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.main.domain.GameEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RunGame(
    private val game: Game,
    private val gameTime: GameTime,
    private val chronicle: GameChronicle
) {
    private val stepChannel = Channel<Unit>(Channel.UNLIMITED)

    // region public

    var stepMode: Boolean = false

    operator fun invoke(): Flow<GameEvent> = flow {
        chronicle.clear()
        emit(GameEvent.Started)
        gameTime.turn = 0

        while (gameTime.phase == GamePhase.CULTIVATION) {
            gameTime.turn++
            chronicle(
                Moment.EVENT_TURN(
                    game.players,
                    totalTimeTakenSeconds = chronicle.timeTaken
                )
            )
            game.runOneCultivationTurn()
            emit(
                GameEvent.TurnComplete(
                    phase = GamePhase.CULTIVATION,
                    playersScoreData = game.score
                )
            )
            game.detectBattlePhase()

            if (stepMode) {
                emit(GameEvent.WaitForStep)
                stepChannel.receive() // Wait for user to continue
            }
        }
        game.setupBattlePhase()

        while (!game.isGameFinished) {
            gameTime.turn++
            chronicle(
                Moment.EVENT_TURN(
                    game.players,
                    totalTimeTakenSeconds = chronicle.timeTaken
                )
            )

            game.runOneBattleTurn()
            emit(
                GameEvent.TurnComplete(
                    phase = GamePhase.BATTLE,
                    playersScoreData = game.score
                )
            )
            if (stepMode) {
                emit(GameEvent.WaitForStep)
                stepChannel.receive() // Wait for user to continue
            }
        }
        val data = game.score
        chronicle(Moment.FINISHED(data, totalTimeTaken = chronicle.timeTaken))
        emit(GameEvent.Completed(data))
    }

    suspend fun continueToNextStep() {
        stepChannel.send(Unit)
    }

    // endregion public

}
