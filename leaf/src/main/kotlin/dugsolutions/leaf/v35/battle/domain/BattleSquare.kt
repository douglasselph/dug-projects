package dugsolutions.leaf.v35.battle.domain

import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter

/**
 * One player's square in one Strike Row.
 *
 * A square may contain at most three dice and any number of Critters. Dice are
 * live references to dice that continue to belong to PlayerDice.hand.
 */
class BattleSquare internal constructor(
    val playerId: PlayerId,
    val row: StrikeRow
) {
    companion object {
        const val MAX_DICE = 3
    }

    private val placedDice = mutableListOf<Die>()
    private val placedCritters = mutableListOf<Critter>()

    /** Defensive structural snapshot containing the live placed dice. */
    val dice: List<Die>
        get() = placedDice.toList()

    /** Defensive structural snapshot of committed Critters. */
    val critters: List<Critter>
        get() = placedCritters.toList()

    val dieCount: Int
        get() = placedDice.size

    val critterCount: Int
        get() = placedCritters.size

    val isFull: Boolean
        get() = dieCount >= MAX_DICE

    val isEmpty: Boolean
        get() = placedDice.isEmpty() && placedCritters.isEmpty()

    internal fun containsDieIdentity(die: Die): Boolean =
        placedDice.any { it === die }

    internal fun addDie(die: Die) {
        stateCheck(
            !isFull,
            context = "BattleSquare"
        ) {
            "Strike Square for player ${playerId.value} row $row already contains $MAX_DICE dice"
        }
        placedDice.add(die)
    }

    internal fun removeDieIdentity(die: Die): Boolean {
        val index = placedDice.indexOfFirst { it === die }
        if (index < 0) return false
        placedDice.removeAt(index)
        return true
    }

    internal fun addCritter(critter: Critter) {
        placedCritters.add(critter)
    }

    internal fun removeCritter(critter: Critter): Boolean =
        placedCritters.remove(critter)

    internal fun drainCritters(): List<Critter> {
        val result = placedCritters.toList()
        placedCritters.clear()
        return result
    }

    internal fun drainDice(): List<Die> {
        val result = placedDice.toList()
        placedDice.clear()
        return result
    }
}
