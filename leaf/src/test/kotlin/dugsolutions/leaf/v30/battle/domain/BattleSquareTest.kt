package dugsolutions.leaf.v30.battle.domain

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.random.die.Die
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BattleSquareTest {

    @Test
    fun constructor_whenIncomingListChanges_keepsOriginalItems() {
        val incoming = mutableListOf<BattleItem>(BattleItem.CritterItem(Critter.BEE))
        val square = BattleSquare(incoming)

        incoming.clear()

        assertEquals(listOf(BattleItem.CritterItem(Critter.BEE)), square.all)
    }

    @Test
    fun constructor_withMoreThanThreeItems_throwsException() {
        assertThrows<IllegalArgumentException> {
            BattleSquare(
                listOf(
                    BattleItem.CritterItem(Critter.BEE),
                    BattleItem.CritterItem(Critter.WORM),
                    BattleItem.CritterItem(Critter.BEE),
                    BattleItem.CritterItem(Critter.WORM)
                )
            )
        }
    }

    @Test
    fun add_appendsItemAndReturnsSameSquare() {
        val square = BattleSquare()
        val item = BattleItem.CritterItem(Critter.WORM)

        val result = square.add(item)

        assertEquals(square, result)
        assertEquals(listOf(item), square.all)
        assertEquals(1, square.size)
        assertFalse(square.isEmpty)
    }

    @Test
    fun add_whenFull_throwsException() {
        val square = BattleSquare(
            listOf(
                BattleItem.CritterItem(Critter.BEE),
                BattleItem.CritterItem(Critter.WORM),
                BattleItem.CritterItem(Critter.BEE)
            )
        )

        assertTrue(square.isFull)
        assertThrows<IllegalArgumentException> {
            square.add(BattleItem.CritterItem(Critter.WORM))
        }
    }

    @Test
    fun remove_whenItemExists_removesIt() {
        val item = BattleItem.CritterItem(Critter.BEE)
        val square = BattleSquare(listOf(item, BattleItem.CritterItem(Critter.WORM)))

        val result = square.remove(item)

        assertTrue(result)
        assertEquals(listOf(BattleItem.CritterItem(Critter.WORM)), square.all)
    }

    @Test
    fun remove_whenItemDoesNotExist_returnsFalse() {
        val square = BattleSquare(listOf(BattleItem.CritterItem(Critter.BEE)))

        val result = square.remove(BattleItem.CritterItem(Critter.WORM))

        assertFalse(result)
        assertEquals(listOf(BattleItem.CritterItem(Critter.BEE)), square.all)
    }

    @Test
    fun clear_removesAllItems() {
        val square = BattleSquare(listOf(BattleItem.CritterItem(Critter.BEE)))

        square.clear()

        assertTrue(square.isEmpty)
        assertEquals(0, square.size)
    }

    @Test
    fun snapshot_copiesDieValue() {
        val die = FixedDie(sides = 8, value = 3)
        val square = BattleSquare(listOf(BattleItem.DieItem(die)))

        val snapshot = square.snapshot()
        die.adjustTo(7)

        val item = assertIs<BattleItemSnapshot.DieItem>(snapshot.items.single())
        assertEquals(8, item.die.sides)
        assertEquals(3, item.die.value)
    }

    private class FixedDie(
        sides: Int,
        value: Int
    ) : Die(sides) {
        init {
            adjustTo(value)
        }

        override fun roll(): Die = this
    }
}
