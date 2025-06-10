package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.random.di.DieFactory
import dugsolutions.leaf.grove.Grove
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectAllADieTest {

    private val sampleDie = SampleDie()
    private val d4: Die = sampleDie.d4
    private val d6: Die = sampleDie.d6
    private val d8: Die = sampleDie.d8
    private val d10: Die = sampleDie.d10
    private val mockGrove = mockk<Grove>(relaxed = true)
    private val mockDieFactory = mockk<DieFactory>(relaxed = true)

    private val SUT = SelectAllDice(mockGrove, mockDieFactory)

    @BeforeEach
    fun setup() {
        every { mockDieFactory(DieSides.D4) } returns d4
        every { mockDieFactory(DieSides.D6) } returns d6
        every { mockDieFactory(DieSides.D8) } returns d8
        every { mockDieFactory(DieSides.D10) } returns d10
        every { mockDieFactory(4) } returns d4
        every { mockDieFactory(6) } returns d6
        every { mockDieFactory(8) } returns d8
        every { mockDieFactory(10) } returns d10
    }

    @Test
    fun invoke_whenGroveHasNoDice_returnsEmptyDiceCollection() {
        // Arrange
        every { mockGrove.getAvailableDiceSides() } returns emptyList()

        // Act
        val result = SUT()

        // Assert
        assertTrue(result.isEmpty(), "Should return empty dice collection when grove has no dice")
        verify { mockGrove.getAvailableDiceSides() }
    }

    @Test
    fun invoke_whenGroveHasSingleDieType_returnsSingleDie() {
        // Arrange
        every { mockGrove.getAvailableDiceSides() } returns listOf(6)
        every { mockGrove.getDiceQuantity(6) } returns 1

        // Act
        val result = SUT()

        // Assert
        assertEquals(1, result.size, "Should return one die when grove has single die")
        assertEquals(d6, result[0])
        verify { mockGrove.getAvailableDiceSides() }
        verify { mockGrove.getDiceQuantity(6) }
        verify { mockDieFactory(6) }
    }

    @Test
    fun invoke_whenGroveHasMultipleSameDice_returnsAllDice() {
        // Arrange
        every { mockGrove.getAvailableDiceSides() } returns listOf(8)
        every { mockGrove.getDiceQuantity(8) } returns 3

        // Act
        val result = SUT()

        // Assert
        assertEquals(3, result.size, "Should return three dice when grove has three of same type")
        assertEquals(d8, result[0])
        assertEquals(d8, result[1])
        assertEquals(d8, result[2])
        verify { mockGrove.getAvailableDiceSides() }
        verify { mockGrove.getDiceQuantity(8) }
        verify(exactly = 3) { mockDieFactory(8) }
    }

    @Test
    fun invoke_whenGroveHasMultipleDifferentDiceTypes_returnsAllDice() {
        // Arrange
        every { mockGrove.getAvailableDiceSides() } returns listOf(4, 6, 10)
        every { mockGrove.getDiceQuantity(4) } returns 1
        every { mockGrove.getDiceQuantity(6) } returns 2
        every { mockGrove.getDiceQuantity(10) } returns 1

        // Act
        val result = SUT()

        // Assert
        assertEquals(4, result.size, "Should return all dice from different types")
        verify { mockGrove.getAvailableDiceSides() }
        verify { mockGrove.getDiceQuantity(4) }
        verify { mockGrove.getDiceQuantity(6) }
        verify { mockGrove.getDiceQuantity(10) }
        verify(exactly = 1) { mockDieFactory(4) }
        verify(exactly = 2) { mockDieFactory(6) }
        verify(exactly = 1) { mockDieFactory(10) }
    }

    @Test
    fun invoke_whenGroveHasZeroQuantityDice_skipsCreation() {
        // Arrange
        every { mockGrove.getAvailableDiceSides() } returns listOf(6)
        every { mockGrove.getDiceQuantity(6) } returns 0

        // Act
        val result = SUT()

        // Assert
        assertTrue(result.isEmpty(), "Should return empty collection when dice quantity is zero")
        verify { mockGrove.getAvailableDiceSides() }
        verify { mockGrove.getDiceQuantity(6) }
        verify(exactly = 0) { mockDieFactory(any<Int>()) }
    }

    @Test
    fun invoke_whenMixedQuantities_createsCorrectAmounts() {
        // Arrange
        every { mockGrove.getAvailableDiceSides() } returns listOf(4, 6, 8)
        every { mockGrove.getDiceQuantity(4) } returns 1
        every { mockGrove.getDiceQuantity(6) } returns 0
        every { mockGrove.getDiceQuantity(8) } returns 4

        // Act
        val result = SUT()

        // Assert
        assertEquals(5, result.size, "Should create correct total number of dice")
        verify { mockGrove.getAvailableDiceSides() }
        verify { mockGrove.getDiceQuantity(4) }
        verify { mockGrove.getDiceQuantity(6) }
        verify { mockGrove.getDiceQuantity(8) }
        verify(exactly = 1) { mockDieFactory(4) }
        verify(exactly = 0) { mockDieFactory(6) }
        verify(exactly = 4) { mockDieFactory(8) }
    }
} 
