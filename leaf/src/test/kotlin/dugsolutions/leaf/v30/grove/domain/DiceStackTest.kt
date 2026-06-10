package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.random.die.DieSides
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiceStackTest {

    @Test
    fun constructor_holdsDieSidesAndDefaultCountIsZero() {
        val stack = DiceStack(DieSides.D6)

        assertEquals(DieSides.D6, stack.sides)
        assertEquals(0, stack.count)
    }

    @Test
    fun constructor_withInitialCount_setsCount() {
        val stack = DiceStack(DieSides.D8, initialCount = 3)

        assertEquals(3, stack.count)
    }

    @Test
    fun setCount_updatesCount() {
        val stack = DiceStack(DieSides.D10)

        stack.setCount(7)

        assertEquals(7, stack.count)
    }

    @Test
    fun add_increasesCount() {
        val stack = DiceStack(DieSides.D12, initialCount = 2)

        stack.add(3)

        assertEquals(5, stack.count)
    }

    @Test
    fun remove_whenEnoughDice_decreasesCountAndReturnsTrue() {
        val stack = DiceStack(DieSides.D20, initialCount = 4)

        val removed = stack.remove(3)

        assertTrue(removed)
        assertEquals(1, stack.count)
    }

    @Test
    fun remove_whenNotEnoughDice_returnsFalseAndLeavesCount() {
        val stack = DiceStack(DieSides.D4, initialCount = 2)

        val removed = stack.remove(3)

        assertFalse(removed)
        assertEquals(2, stack.count)
    }

    @Test
    fun negativeCountsOrAmounts_throwException() {
        val stack = DiceStack(DieSides.D6)

        assertThrows<IllegalArgumentException> { DiceStack(DieSides.D6, initialCount = -1) }
        assertThrows<IllegalArgumentException> { stack.setCount(-1) }
        assertThrows<IllegalArgumentException> { stack.add(-1) }
        assertThrows<IllegalArgumentException> { stack.remove(-1) }
    }

}
