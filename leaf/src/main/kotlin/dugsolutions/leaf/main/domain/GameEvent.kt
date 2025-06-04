package dugsolutions.leaf.main.domain

import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.player.domain.PlayersScoreData


sealed class GameEvent {
    data object Started : GameEvent()
    data class TurnComplete(
        val phase: GamePhase,
        val playersScoreData: PlayersScoreData
    ) : GameEvent()

    data class Completed(val result: PlayersScoreData) : GameEvent()
    data object WaitForStep : GameEvent()
}
