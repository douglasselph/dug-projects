package dugsolutions.leaf.player.components

import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.player.Player

data class PlayersScoreData(
    val turn: Int,
    val players: List<PlayerScoreData>
) {
    val winner: PlayerScoreData?
        get() = players.maxByOrNull { it.player.score.scoreDice }
}

data class PlayerScoreData(
    val player: Player,
    val score: PlayerScore
)
