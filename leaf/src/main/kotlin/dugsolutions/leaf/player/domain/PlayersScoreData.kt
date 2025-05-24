package dugsolutions.leaf.player.domain

import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.player.Player

data class PlayersScoreData(
    val turn: Int,
    val players: List<PlayerScoreData>
) {
    val winner: PlayerScoreData?
        get() = players.maxByOrNull { it.player.score.scoreDice }

    override fun toString(): String {
        val players = players.map { it.player.id.toString() }
        val playersLine = players.joinToString(",")
        return "PlayersScoreData(players=$playersLine, turn=$turn, winner=$winner)"
    }

}

data class PlayerScoreData(
    val player: Player,
    val score: PlayerScore
) {
    override fun toString(): String {
        return "PlayerScoreData(player=${player.id}, score=$score)"
    }
}
