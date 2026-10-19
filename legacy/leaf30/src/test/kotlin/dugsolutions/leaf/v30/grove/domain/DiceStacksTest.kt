package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.random.die.DieSides
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DiceStacksTest {

    private lateinit var SUT: DiceStacks

    @BeforeEach
    fun setup() {
        SUT = DiceStacks()
    }

    @Test
    fun constructor_createsOneStackForEachDieSides() {
        val stacks = SUT.asList()

        assertEquals(DieSides.entries.size, stacks.size)
        DieSides.entries.forEach { sides ->
            assertEquals(sides, SUT.getStack(sides).sides)
            assertEquals(0, SUT.getCount(sides))
        }
    }

    @Test
    fun getStack_returnsStableStackForDieSides() {
        val first = SUT.getStack(DieSides.D8)
        val second = SUT.getStack(DieSides.D8)

        assertSame(first, second)
    }

    @Test
    fun add_incrementsMatchingStack() {
        val stack = SUT.add(DieSides.D6, amount = 3)

        assertEquals(DieSides.D6, stack.sides)
        assertEquals(3, SUT.getCount(DieSides.D6))
        assertEquals(0, SUT.getCount(DieSides.D8))
    }

    @Test
    fun add_withDefaultAmount_incrementsByOne() {
        SUT.add(DieSides.D4)

        assertEquals(1, SUT.getCount(DieSides.D4))
    }

    @Test
    fun remove_whenEnoughDice_decrementsMatchingStack() {
        SUT.add(DieSides.D10, amount = 4)

        val removed = SUT.remove(DieSides.D10, amount = 2)

        assertTrue(removed)
        assertEquals(2, SUT.getCount(DieSides.D10))
    }

    @Test
    fun remove_whenNotEnoughDice_returnsFalseAndLeavesCount() {
        SUT.add(DieSides.D12, amount = 2)

        val removed = SUT.remove(DieSides.D12, amount = 3)

        assertFalse(removed)
        assertEquals(2, SUT.getCount(DieSides.D12))
    }

    @Test
    fun setCount_updatesMatchingStack() {
        SUT.setCount(DieSides.D20, 5)

        assertEquals(5, SUT.getCount(DieSides.D20))
        assertEquals(0, SUT.getCount(DieSides.D4))
    }

    @Test
    fun negativeAmounts_throwException() {
        assertThrows<IllegalArgumentException> { SUT.add(DieSides.D4, amount = -1) }
        assertThrows<IllegalArgumentException> { SUT.remove(DieSides.D4, amount = -1) }
        assertThrows<IllegalArgumentException> { SUT.setCount(DieSides.D4, -1) }
    }

}
