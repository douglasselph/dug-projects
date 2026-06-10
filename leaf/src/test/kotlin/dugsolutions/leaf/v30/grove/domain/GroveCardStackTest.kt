package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Commons
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GroveCardStackTest {

    private lateinit var rootFiveOne: GameCard
    private lateinit var rootFiveTwo: GameCard

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        rootFiveOne = requireNotNull(registry.getCard("Root_05_01"))
        rootFiveTwo = requireNotNull(registry.getCard("Root_05_02"))
    }

    @Test
    fun constructor_holdsCardAndDefaultCountIsZero() {
        val stack = GroveCardStack(rootFiveOne)

        assertSame(rootFiveOne, stack.card)
        assertEquals(0, stack.count)
    }

    @Test
    fun constructor_withInitialCount_setsCount() {
        val stack = GroveCardStack(rootFiveOne, initialCount = 3)

        assertEquals(3, stack.count)
    }

    @Test
    fun setCount_updatesCount() {
        val stack = GroveCardStack(rootFiveOne)

        stack.setCount(7)

        assertEquals(7, stack.count)
    }

    @Test
    fun add_increasesCount() {
        val stack = GroveCardStack(rootFiveOne, initialCount = 2)

        stack.add(3)

        assertEquals(5, stack.count)
    }

    @Test
    fun remove_whenEnoughCards_decreasesCountAndReturnsTrue() {
        val stack = GroveCardStack(rootFiveOne, initialCount = 4)

        val removed = stack.remove(3)

        assertTrue(removed)
        assertEquals(1, stack.count)
    }

    @Test
    fun remove_whenNotEnoughCards_returnsFalseAndLeavesCount() {
        val stack = GroveCardStack(rootFiveOne, initialCount = 2)

        val removed = stack.remove(3)

        assertFalse(removed)
        assertEquals(2, stack.count)
    }

    @Test
    fun requireSameCard_whenDifferentCardWithSameTypeAndCost_throwsException() {
        val stack = GroveCardStack(rootFiveOne)

        assertThrows<IllegalArgumentException> {
            stack.requireSameCard(rootFiveTwo)
        }
    }

    @Test
    fun negativeCountsOrAmounts_throwException() {
        val stack = GroveCardStack(rootFiveOne)

        assertThrows<IllegalArgumentException> { GroveCardStack(rootFiveOne, initialCount = -1) }
        assertThrows<IllegalArgumentException> { stack.setCount(-1) }
        assertThrows<IllegalArgumentException> { stack.add(-1) }
        assertThrows<IllegalArgumentException> { stack.remove(-1) }
    }

}
