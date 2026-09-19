package dugsolutions.leaf.v14.chronicle.domain

import dugsolutions.leaf.v14.game.Game

class PlayerUnderTest(
    private val game: Game
) {

    val playerId: Int
        get() = game.players.minByOrNull { it.id }?.id ?: game.players[0].id

}