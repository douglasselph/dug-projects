package dugsolutions.leaf.v30.table

import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player

class Table(
    val grove: Grove
) {
    private val _players = mutableListOf<Player>()

    val players: List<Player>
        get() = _players.toList()

    fun add(player: Player): Table {
        _players.add(player)
        return this
    }
}
