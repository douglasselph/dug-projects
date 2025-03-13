package dugsolutions.leaf.game

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTurn
import dugsolutions.leaf.simulator.GameEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class RunGame(
    private val game: Game,
    private val gameTurn: GameTurn,
    private val chronicle: GameChronicle
) {

    private val stepChannel = Channel<Unit>(Channel.UNLIMITED)
    private val _enableStepMode = MutableStateFlow(false)

    operator fun invoke(): Flow<GameEvent> = flow {

        chronicle.clear()

        emit(GameEvent.Started)

        gameTurn.turn = 0

        while (game.inCultivationPhase) {
            gameTurn.turn++
            chronicle(GameChronicle.Moment.EVENT_TURN(game.players))
            game.runOneCultivationTurn()
            emit(
                GameEvent.TurnProgress(
                    phase = GamePhase.CULTIVATION,
                    playersScoreData = game.score
                )
            )
            game.detectBattlePhase()

            if (_enableStepMode.value) {
                emit(GameEvent.WaitForStep)
                stepChannel.receive() // Wait for user to continue
            }
        }
        game.setupBattlePhase()

        chronicle(GameChronicle.Moment.EVENT_BATTLE(game.score))

        while (!game.isGameFinished) {
            gameTurn.turn++
            chronicle(GameChronicle.Moment.EVENT_TURN(game.players))
            game.runOneBattleTurn()
            emit(
                GameEvent.TurnProgress(
                    phase = GamePhase.BATTLE,
                    playersScoreData = game.score
                )
            )
            if (_enableStepMode.value) {
                emit(GameEvent.WaitForStep)
                stepChannel.receive() // Wait for user to continue
            }
        }
        val data = game.score
        chronicle(GameChronicle.Moment.FINISHED(data))
        emit(GameEvent.Completed(data))
    }

    var stepMode: Boolean
        get() = _enableStepMode.value
        set(value) {
            _enableStepMode.value = value
        }

    // Method to continue to next step when in step mode
    suspend fun continueToNextStep() {
        stepChannel.send(Unit)
    }
}