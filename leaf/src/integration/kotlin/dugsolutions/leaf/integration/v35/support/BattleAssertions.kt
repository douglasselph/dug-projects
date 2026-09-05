package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.tokens.Critter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Reusable assertions over transient Battle snapshots. */
object BattleAssertions {
    fun assertOrder(snapshot: BattleSnapshot, vararg playerIds: Int) {
        assertEquals(playerIds.toList(), snapshot.order.map { it.value })
        playerIds.forEachIndexed { index, id ->
            assertEquals(index, snapshot.column(id).columnIndex)
        }
    }

    fun assertDieValues(
        snapshot: BattleSnapshot,
        playerId: Int,
        row: StrikeRow,
        vararg values: Int
    ) {
        assertEquals(
            values.toList(),
            snapshot.square(playerId, row).dieValues,
            "Unexpected dice for P$playerId $row"
        )
    }

    fun assertCritters(
        snapshot: BattleSnapshot,
        playerId: Int,
        row: StrikeRow,
        vararg critters: Critter
    ) {
        assertEquals(
            critters.toList(),
            snapshot.square(playerId, row).critters,
            "Unexpected Critters for P$playerId $row"
        )
    }

    fun assertClosed(snapshot: BattleSnapshot, row: StrikeRow) {
        assertTrue(row in snapshot.closedRows, "Expected $row to be globally closed")
    }

    fun assertOpen(snapshot: BattleSnapshot, row: StrikeRow) {
        assertFalse(row in snapshot.closedRows, "Expected $row to remain globally open")
    }

    fun assertWithdrawn(snapshot: BattleSnapshot, playerId: Int, row: StrikeRow) {
        assertTrue(
            row in snapshot.column(PlayerId(playerId)).withdrawnRows,
            "Expected P$playerId to be withdrawn from $row"
        )
    }
}
