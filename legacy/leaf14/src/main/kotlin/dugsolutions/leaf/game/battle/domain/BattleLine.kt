package dugsolutions.leaf.game.battle.domain

import dugsolutions.leaf.player.Player

class BattleLine(
    val line: MutableList<PlayerValue>
) {
    /**
     * Sorts the line from lowest to highest attack value.
     */
    fun sort() {
        line.sortBy { it.attack }
    }

    /**
     * Returns the next value(s) from the line and removes them.
     * Returns a list to handle ties (multiple players with same attack).
     * Returns null if the line is empty.
     */
    fun next(): List<PlayerValue>? {
        if (line.isEmpty()) return null

        val firstAttack = line.first().attack
        val matching = line.filter { it.attack == firstAttack }
        line.removeAll(matching)
        return matching
    }
}

data class PlayerValue(
    val player: Player,
    val dieValue: DieBoosted
) {
    val attack: Int
        get() = dieValue.attack
}
