package dugsolutions.leaf.simulator

import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.player.components.PlayerScoreData
import dugsolutions.leaf.player.components.PlayersScoreData


sealed class GameEvent {
    data object Started : GameEvent()
    data class TurnProgress(
            val phase: GamePhase,
            val playersScoreData: PlayersScoreData
    ) : GameEvent()
    data class Completed(val result: PlayersScoreData) : GameEvent()
    data object WaitForStep : GameEvent()
}