package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.tokens.Critter

/** Immutable view of one Strike Square at a particular integration-test moment. */
data class BattleSquareSnapshot(
    val dice: List<DieSnapshot>,
    val critters: List<Critter>
) {
    val dieValues: List<Int>
        get() = immutableList(dice.map { it.value })

    val totalCritters: Int
        get() = critters.size
}

/** Immutable view of one player's Battle Grid column. */
data class BattleColumnSnapshot(
    val playerId: PlayerId,
    val columnIndex: Int,
    val withdrawnRows: Set<StrikeRow>,
    val squares: Map<StrikeRow, BattleSquareSnapshot>
) {
    fun square(row: StrikeRow): BattleSquareSnapshot =
        requireNotNull(squares[row]) { "Missing Battle square $row for player ${playerId.value}" }
}

/**
 * Test-only immutable snapshot of transient per-round Battle state.
 *
 * [GameSnapshot] intentionally contains durable Game/Player/Grove state only;
 * this companion snapshot captures the live Grid while a stepwise Battle round
 * is in progress.
 */
data class BattleSnapshot(
    val order: List<PlayerId>,
    val closedRows: Set<StrikeRow>,
    val columns: Map<PlayerId, BattleColumnSnapshot>
) {
    fun column(playerId: PlayerId): BattleColumnSnapshot =
        requireNotNull(columns[playerId]) {
            "No Battle column for player ${playerId.value}"
        }

    fun column(playerId: Int): BattleColumnSnapshot =
        column(PlayerId(playerId))

    fun square(playerId: PlayerId, row: StrikeRow): BattleSquareSnapshot =
        column(playerId).square(row)

    fun square(playerId: Int, row: StrikeRow): BattleSquareSnapshot =
        square(PlayerId(playerId), row)

    companion object {
        fun capture(state: BattleState): BattleSnapshot {
            val order = state.playerIdsInBattleOrder
            return BattleSnapshot(
                order = immutableList(order),
                closedRows = StrikeRow.entries
                    .filter(state.grid::isRowClosed)
                    .let(::immutableSet),
                columns = immutableMap(
                    order.associateWith { playerId ->
                        val column = state.grid.column(playerId)
                        BattleColumnSnapshot(
                            playerId = playerId,
                            columnIndex = column.index,
                            withdrawnRows = StrikeRow.entries
                                .filter { state.grid.isPlayerWithdrawn(playerId, it) }
                                .let(::immutableSet),
                            squares = immutableMap(
                                StrikeRow.entries.associateWith { row ->
                                    val square = state.grid.square(playerId, row)
                                    BattleSquareSnapshot(
                                        dice = immutableList(square.dice.map(DieSnapshot::capture)),
                                        critters = immutableList(square.critters)
                                    )
                                }
                            )
                        )
                    }
                )
            )
        }
    }
}
