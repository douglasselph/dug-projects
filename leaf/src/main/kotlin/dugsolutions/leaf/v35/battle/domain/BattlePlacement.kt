package dugsolutions.leaf.v35.battle.domain

import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter

/** Location of one item on the Battle Grid. */
data class BattleLocation(
    val playerId: PlayerId,
    val row: StrikeRow
)

/**
 * Live placement view for one die.
 *
 * The [die] is deliberately the same live Die object that remains in the
 * owning Player's Dice Hand. Battle placement adds location; it is not a
 * fourth PlayerDice ownership zone.
 *
 * This is intentionally not a data class. Die equality is value-based and a
 * die's value is mutable, so placement identity must never depend on Die.equals.
 */
class BattleDiePlacement internal constructor(
    val playerId: PlayerId,
    val row: StrikeRow,
    val die: Die
) {
    val location: BattleLocation
        get() = BattleLocation(playerId, row)

    override fun toString(): String =
        "BattleDiePlacement(playerId=${playerId.value}, row=$row, die=$die)"
}

/** One Critter committed to a player's Strike Square. */
data class BattleCritterPlacement(
    val playerId: PlayerId,
    val row: StrikeRow,
    val critter: Critter
) {
    val location: BattleLocation
        get() = BattleLocation(playerId, row)
}
