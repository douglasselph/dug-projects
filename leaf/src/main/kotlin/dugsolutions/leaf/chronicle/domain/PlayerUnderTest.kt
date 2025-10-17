package dugsolutions.leaf.chronicle.domain

import dugsolutions.leaf.game.Game

class PlayerUnderTest(
    private val game: Game
) {

    val playerId: Int
        get() = game.players.minByOrNull { it.id }?.id ?: game.players[0].id

}