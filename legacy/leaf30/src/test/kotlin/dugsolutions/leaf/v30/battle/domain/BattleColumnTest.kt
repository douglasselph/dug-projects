package dugsolutions.leaf.v30.battle.domain

import dugsolutions.leaf.v30.common.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleColumnTest {

    @Test
    fun constructor_createsEmptySquareForEachStrikeRow() {
        val column = BattleColumn(playerId = 9)

        BattleStrikeRow.entries.forEach { row ->
            assertTrue(column.get(row).isEmpty)
        }
    }

    @Test
    fun add_placesItemInSelectedRowAndReturnsSameColumn() {
        val column = BattleColumn(playerId = 9)
        val item = BattleItem.CritterItem(Critter.BEE)

        val result = column.add(BattleStrikeRow.STRIKE_2, item)

        assertEquals(column, result)
        assertEquals(listOf(item), column.get(BattleStrikeRow.STRIKE_2).all)
        assertTrue(column.get(BattleStrikeRow.STRIKE_1).isEmpty)
        assertTrue(column.get(BattleStrikeRow.STRIKE_3).isEmpty)
    }

    @Test
    fun remove_whenItemExists_removesItFromSelectedRow() {
        val column = BattleColumn(playerId = 9)
        val item = BattleItem.CritterItem(Critter.WORM)
        column.add(BattleStrikeRow.STRIKE_3, item)

        val result = column.remove(BattleStrikeRow.STRIKE_3, item)

        assertTrue(result)
        assertTrue(column.get(BattleStrikeRow.STRIKE_3).isEmpty)
    }

    @Test
    fun remove_whenItemDoesNotExist_returnsFalse() {
        val column = BattleColumn(playerId = 9)

        val result = column.remove(BattleStrikeRow.STRIKE_1, BattleItem.CritterItem(Critter.BEE))

        assertFalse(result)
    }

    @Test
    fun clear_removesItemsFromEveryRow() {
        val column = BattleColumn(playerId = 9)
        column.add(BattleStrikeRow.STRIKE_1, BattleItem.CritterItem(Critter.BEE))
        column.add(BattleStrikeRow.STRIKE_2, BattleItem.CritterItem(Critter.WORM))

        column.clear()

        BattleStrikeRow.entries.forEach { row ->
            assertTrue(column.get(row).isEmpty)
        }
    }

    @Test
    fun snapshot_containsPlayerIdAndEveryRow() {
        val column = BattleColumn(playerId = 9)
        val item = BattleItem.CritterItem(Critter.BEE)
        column.add(BattleStrikeRow.STRIKE_1, item)

        val snapshot = column.snapshot()

        assertEquals(9, snapshot.playerId)
        assertEquals(BattleStrikeRow.entries.toSet(), snapshot.squares.keys)
        assertEquals(BattleItemSnapshot.CritterItem(Critter.BEE), snapshot.squares.getValue(BattleStrikeRow.STRIKE_1).items.single())
        assertTrue(snapshot.squares.getValue(BattleStrikeRow.STRIKE_2).items.isEmpty())
        assertTrue(snapshot.squares.getValue(BattleStrikeRow.STRIKE_3).items.isEmpty())
    }
}
