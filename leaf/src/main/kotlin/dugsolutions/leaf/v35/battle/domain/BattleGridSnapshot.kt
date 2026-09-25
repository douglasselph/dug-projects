package dugsolutions.leaf.v35.battle.domain

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter

/** Immutable reporting snapshot of one die currently placed in a Strike Square. */
data class BattleGridDieSnapshot(
    val sides: DieSides,
    val value: Int
)

/** Immutable reporting snapshot of one committed Critter and its current effective value. */
data class BattleGridCritterSnapshot(
    val critter: Critter,
    val value: Int
)

/** Immutable reporting snapshot of one player's square in one Strike Row. */
data class BattleGridSquareSnapshot(
    val playerId: PlayerId,
    val dice: List<BattleGridDieSnapshot>,
    val critters: List<BattleGridCritterSnapshot>,
    val withdrawn: Boolean = false
) {
    val total: Int
        get() = dice.sumOf { it.value } + critters.sumOf { it.value }
}

/** Immutable reporting snapshot of all player squares in one Strike Row. */
data class BattleGridRowSnapshot(
    val row: StrikeRow,
    val squares: List<BattleGridSquareSnapshot>
)

/**
 * Captures the live Battle Grid in stable Battle order for Chronicle/reporting.
 *
 * Dice and Critter values are copied at capture time so later rerolls, value
 * changes, cleanup, or temporary Critter-value resets cannot alter history.
 */
object BattleGridSnapshot {
    fun rows(battleState: BattleState): List<BattleGridRowSnapshot> =
        StrikeRow.entries.map { row(battleState, it) }

    fun row(
        battleState: BattleState,
        row: StrikeRow
    ): BattleGridRowSnapshot =
        BattleGridRowSnapshot(
            row = row,
            squares = battleState.playersInBattleOrder.map { player ->
                val square = battleState.grid.square(player.id, row)
                BattleGridSquareSnapshot(
                    playerId = player.id,
                    dice = square.dice.map { die ->
                        BattleGridDieSnapshot(
                            sides = DieSides.from(die.sides),
                            value = die.value
                        )
                    },
                    critters = square.critters.map { critter ->
                        BattleGridCritterSnapshot(
                            critter = critter,
                            value = player.critterValues.valueOf(critter)
                        )
                    },
                    withdrawn = battleState.grid.isPlayerWithdrawn(player.id, row)
                )
            }
        )
}
