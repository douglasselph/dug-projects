package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.factory.DieFactory
import dugsolutions.leaf.grove.Grove
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectPossibleBestDiceTest {

    // Test subject
    private lateinit var SUT: SelectPossibleDice
    
    // Dependencies
    private lateinit var grove: Grove
    private lateinit var dieFactory: DieFactory
    
    @BeforeEach
    fun setup() {
        // Create mock dependencies
        grove = mockk(relaxed = true)
        dieFactory = mockk(relaxed = true)
        
        // Create the test subject
        SUT = SelectPossibleDice(grove, dieFactory)
    }
    
    @Test
    fun invoke_whenDiceAvailable_returnsAllAvailableDice() {
        // Arrange
        val sides = listOf(DieSides.D4.value, DieSides.D6.value, DieSides.D8.value)
        val d4 = mockk<Die>(relaxed = true)
        val d6 = mockk<Die>(relaxed = true)
        val d8 = mockk<Die>(relaxed = true)
        
        every { grove.getAvailableDiceSides() } returns sides
        every { dieFactory(DieSides.D4.value) } returns d4
        every { dieFactory(DieSides.D6.value) } returns d6
        every { dieFactory(DieSides.D8.value) } returns d8
        
        // Act
        val result = SUT()
        
        // Assert
        assertEquals(3, result.size)
        assertTrue(result.contains(d4))
        assertTrue(result.contains(d6))
        assertTrue(result.contains(d8))
        verify { grove.getAvailableDiceSides() }
        verify { dieFactory(DieSides.D4.value) }
        verify { dieFactory(DieSides.D6.value) }
        verify { dieFactory(DieSides.D8.value) }
    }
    
    @Test
    fun invoke_whenNoDiceAvailable_returnsEmptyList() {
        // Arrange
        every { grove.getAvailableDiceSides() } returns emptyList()
        
        // Act
        val result = SUT()
        
        // Assert
        assertTrue(result.isEmpty())
        verify { grove.getAvailableDiceSides() }
        verify(exactly = 0) { dieFactory(any<Int>()) }
    }
    
    @Test
    fun invoke_withMultipleDice_createsEachDieCorrectly() {
        // Arrange
        val sides = listOf(DieSides.D6.value, DieSides.D20.value, DieSides.D10.value)
        val d6 = mockk<Die>(relaxed = true)
        val d20 = mockk<Die>(relaxed = true)
        val d10 = mockk<Die>(relaxed = true)
        
        every { grove.getAvailableDiceSides() } returns sides
        every { dieFactory(DieSides.D6.value) } returns d6
        every { dieFactory(DieSides.D20.value) } returns d20
        every { dieFactory(DieSides.D10.value) } returns d10
        
        // Act
        val result = SUT()
        
        // Assert
        assertEquals(3, result.size)
        assertEquals(d6, result[0])
        assertEquals(d20, result[1])
        assertEquals(d10, result[2])
    }
} 
