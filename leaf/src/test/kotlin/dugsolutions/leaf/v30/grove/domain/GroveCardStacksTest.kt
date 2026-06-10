package dugsolutions.leaf.v30.grove.domain

import dugsolutions.leaf.v30.cards.GameCardRegistry
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.common.Commons
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GroveCardStacksTest {

    private lateinit var rootFiveOne: GameCard
    private lateinit var rootFiveTwo: GameCard
    private lateinit var flowerFourteenOne: GameCard
    private lateinit var SUT: GroveCardStacks

    @BeforeEach
    fun setup() {
        val registry = GameCardRegistry()
        registry.loadFromCsv(Commons.CARD_LIST)
        rootFiveOne = requireNotNull(registry.getCard("Root_05_01"))
        rootFiveTwo = requireNotNull(registry.getCard("Root_05_02"))
        flowerFourteenOne = requireNotNull(registry.getCard("Flower_14_01"))
        SUT = GroveCardStacks()
    }

    @Test
    fun add_withCard_createsMatchingStack() {
        val stack = SUT.add(rootFiveOne)

        assertSame(rootFiveOne, stack.card)
        assertEquals(1, stack.count)
        assertSame(rootFiveOne, SUT.getCard(GroveCardStackID.ROOT_5))
        assertEquals(1, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun add_withSameCard_incrementsExistingStack() {
        SUT.add(rootFiveOne)
        SUT.add(rootFiveOne, amount = 3)

        assertEquals(4, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun add_withDifferentCardInSameTypeAndCost_throwsExceptionAndLeavesStackUnchanged() {
        SUT.add(rootFiveOne, amount = 2)

        assertThrows<IllegalArgumentException> {
            SUT.add(rootFiveTwo)
        }

        assertSame(rootFiveOne, SUT.getCard(GroveCardStackID.ROOT_5))
        assertEquals(2, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun add_withDifferentMatchingStack_createsSeparateStack() {
        SUT.add(rootFiveOne)
        SUT.add(flowerFourteenOne, amount = 2)

        assertSame(rootFiveOne, SUT.getCard(GroveCardStackID.ROOT_5))
        assertSame(flowerFourteenOne, SUT.getCard(GroveCardStackID.FLOWER_14))
        assertEquals(1, SUT.getCount(GroveCardStackID.ROOT_5))
        assertEquals(2, SUT.getCount(GroveCardStackID.FLOWER_14))
    }


    @Test
    fun reset_replacesStackCardAndCount() {
        SUT.add(rootFiveOne, amount = 2)

        val stack = SUT.reset(rootFiveTwo, amount = 8)

        assertSame(rootFiveTwo, stack.card)
        assertSame(rootFiveTwo, SUT.getCard(GroveCardStackID.ROOT_5))
        assertEquals(8, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun reset_withNegativeAmount_throwsException() {
        assertThrows<IllegalArgumentException> {
            SUT.reset(rootFiveOne, amount = -1)
        }
    }

    @Test
    fun remove_withCard_decrementsMatchingStack() {
        SUT.add(rootFiveOne, amount = 4)

        val removed = SUT.remove(rootFiveOne, amount = 2)

        assertTrue(removed)
        assertEquals(2, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun remove_withStackId_decrementsMatchingStack() {
        SUT.add(rootFiveOne, amount = 4)

        val removed = SUT.remove(GroveCardStackID.ROOT_5, amount = 3)

        assertTrue(removed)
        assertEquals(1, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun remove_whenStackIsEmpty_returnsFalse() {
        val removed = SUT.remove(GroveCardStackID.ROOT_5)

        assertFalse(removed)
        assertEquals(0, SUT.getCount(GroveCardStackID.ROOT_5))
        assertNull(SUT.getCard(GroveCardStackID.ROOT_5))
    }

    @Test
    fun remove_whenNotEnoughCards_returnsFalseAndLeavesCount() {
        SUT.add(rootFiveOne, amount = 2)

        val removed = SUT.remove(rootFiveOne, amount = 3)

        assertFalse(removed)
        assertEquals(2, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun remove_withDifferentCardInSameTypeAndCost_throwsException() {
        SUT.add(rootFiveOne, amount = 2)

        assertThrows<IllegalArgumentException> {
            SUT.remove(rootFiveTwo)
        }
    }

    @Test
    fun setCount_updatesExistingStackCount() {
        SUT.add(rootFiveOne)

        SUT.setCount(GroveCardStackID.ROOT_5, 6)

        assertEquals(6, SUT.getCount(GroveCardStackID.ROOT_5))
    }

    @Test
    fun setCount_whenStackIsEmpty_throwsException() {
        assertThrows<IllegalArgumentException> {
            SUT.setCount(GroveCardStackID.ROOT_5, 1)
        }
    }

    @Test
    fun negativeAmounts_throwException() {
        SUT.add(rootFiveOne)

        assertThrows<IllegalArgumentException> { SUT.add(rootFiveOne, amount = -1) }
        assertThrows<IllegalArgumentException> { SUT.remove(rootFiveOne, amount = -1) }
        assertThrows<IllegalArgumentException> { SUT.remove(GroveCardStackID.ROOT_5, amount = -1) }
        assertThrows<IllegalArgumentException> { SUT.setCount(GroveCardStackID.ROOT_5, -1) }
    }

}
