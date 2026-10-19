package dugsolutions.leaf.v30.battle.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.DieSides
import dugsolutions.leaf.v30.random.die.di.DieFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BattleGridTest {

    @Test
    fun constructor_withFourUniquePlayers_createsColumns() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))

        assertEquals(listOf(1, 2, 3, 4), grid.playerIdsInGridOrder)
        assertTrue(grid.getSquare(2, BattleStrikeRow.STRIKE_1).isEmpty)
    }

    @Test
    fun constructor_withWrongNumberOfPlayers_throwsException() {
        assertThrows<IllegalArgumentException> {
            BattleGrid(listOf(1, 2, 3))
        }
    }

    @Test
    fun constructor_withDuplicatePlayerIds_throwsException() {
        assertThrows<IllegalArgumentException> {
            BattleGrid(listOf(1, 2, 2, 4))
        }
    }

    @Test
    fun add_placesItemInSelectedPlayerRowSquare() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        val item = BattleItem.CritterItem(Critter.BEE)

        val result = grid.add(3, BattleStrikeRow.STRIKE_2, item)

        assertEquals(grid, result)
        assertEquals(listOf(item), grid.getSquare(3, BattleStrikeRow.STRIKE_2).all)
        assertTrue(grid.getSquare(3, BattleStrikeRow.STRIKE_1).isEmpty)
    }

    @Test
    fun add_whenSquareAlreadyHasThreeItems_throwsException() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        val row = BattleStrikeRow.STRIKE_1
        grid.add(1, row, BattleItem.CritterItem(Critter.BEE))
        grid.add(1, row, BattleItem.CritterItem(Critter.WORM))
        grid.add(1, row, BattleItem.CritterItem(Critter.BEE))

        assertTrue(grid.getSquare(1, row).isFull)
        assertThrows<IllegalArgumentException> {
            grid.add(1, row, BattleItem.CritterItem(Critter.WORM))
        }
    }

    @Test
    fun remove_whenItemExists_removesIt() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        val item = BattleItem.CritterItem(Critter.WORM)
        grid.add(4, BattleStrikeRow.STRIKE_3, item)

        val result = grid.remove(4, BattleStrikeRow.STRIKE_3, item)

        assertTrue(result)
        assertTrue(grid.getSquare(4, BattleStrikeRow.STRIKE_3).isEmpty)
    }

    @Test
    fun remove_whenItemDoesNotExist_returnsFalse() {
        val grid = BattleGrid(listOf(1, 2, 3, 4))

        val result = grid.remove(1, BattleStrikeRow.STRIKE_1, BattleItem.CritterItem(Critter.BEE))

        assertFalse(result)
    }

    @Test
    fun snapshot_capturesDieValueWithoutTrackingFutureDieMutation() {
        val die = DieFactory(FixedRandomizer(4))(DieSides.D6).adjustTo(2)
        val grid = BattleGrid(listOf(1, 2, 3, 4))
        grid.add(1, BattleStrikeRow.STRIKE_1, BattleItem.DieItem(die))

        val snapshot = grid.snapshot()
        die.adjustTo(6)

        val item = snapshot.columns
            .single { it.playerId == 1 }
            .squares
            .getValue(BattleStrikeRow.STRIKE_1)
            .items
            .single()
        val dieItem = assertIs<BattleItemSnapshot.DieItem>(item)
        assertEquals(6, dieItem.die.sides)
        assertEquals(2, dieItem.die.value)
    }

    private class FixedRandomizer(
        private val value: Int
    ) : Randomizer {
        override fun nextBoolean(): Boolean = value % 2 == 0
        override fun nextInt(from: Int, until: Int): Int = value.coerceIn(from, until - 1)
        override fun nextInt(until: Int): Int = value.coerceIn(0, until - 1)
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
