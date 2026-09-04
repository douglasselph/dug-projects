package dugsolutions.leaf.v35.battle.domain

import dugsolutions.leaf.v35.player.PlayerId

/**
 * One claimed Battle Grid column.
 *
 * [index] is zero-based from left to right. The player occupying column 0 is
 * therefore first in Battle order for the round.
 */
data class BattleColumn(
    val index: Int,
    val playerId: PlayerId
) {
    init {
        require(index >= 0) {
            "Battle column index cannot be negative: $index"
        }
    }
}
