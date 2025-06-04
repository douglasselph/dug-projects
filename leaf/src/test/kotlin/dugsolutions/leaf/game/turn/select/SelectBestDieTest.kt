package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.di.factory.DieFactory
import dugsolutions.leaf.game.acquire.domain.Credit
import dugsolutions.leaf.game.acquire.domain.Credits
import dugsolutions.leaf.grove.Grove
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectBestDieTest {

    private lateinit var grove: Grove
    private lateinit var dieFactory: DieFactory
    private val sampleDie = SampleDie()
    private val dieCost = DieCost()

    private lateinit var SUT: SelectBestDie

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        grove = mockk(relaxed = true)
        dieFactory = mockk(relaxed = true)

        // Create SelectBestDie instance
        SUT = SelectBestDie(grove, dieFactory, dieCost)
    }

    @Test
    fun invoke_whenAffordableHighSidedDieAvailable_returnsHighestAffordableDie() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(8)))
        val sampleDie = sampleDie.d8
        val availableDieSides = listOf(DieSides.D4.value, DieSides.D8.value, DieSides.D6.value, DieSides.D12.value)
        every { grove.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D8.value) } returns sampleDie

        // Act
        val result = SUT(credits)

        // Assert
        assertEquals(sampleDie, result)
    }

    @Test
    fun invoke_whenOnlyOneDieAffordable_returnsThatDie() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(6)))
        val sampleDie = sampleDie.d6
        val availableDieSides = listOf(DieSides.D6.value, DieSides.D8.value, DieSides.D10.value)
        every { grove.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D6.value) } returns sampleDie

        // Act
        val result = SUT(credits)

        // Assert
        assertEquals(sampleDie, result)
    }

    @Test
    fun invoke_whenNoDiceAffordable_returnsNull() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(3)))
        val availableDieSides = listOf(DieSides.D4.value, DieSides.D6.value, DieSides.D8.value)
        every { grove.getAvailableDiceSides() } returns availableDieSides

        // Act
        val result = SUT(credits)

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenNoDiceAvailable_returnsNull() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(10)))
        every { grove.getAvailableDiceSides() } returns emptyList()

        // Act
        val result = SUT(credits)

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenCostExactlyEqualsToDieSides_returnsMatchingDie() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(10)))
        val sampleDie = sampleDie.d10
        val availableDieSides = listOf(
            DieSides.D4.value, DieSides.D10.value, DieSides.D12.value, DieSides.D20.value
        )
        every { grove.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D10.value) } returns sampleDie

        // Act
        val result = SUT(credits)

        // Assert
        assertEquals(sampleDie, result)
    }

    @Test
    fun invoke_whenCostHigherThanAllDice_returnsHighestDie() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(30)))
        val availableDieSides = listOf(
            DieSides.D4.value, DieSides.D12.value, DieSides.D20.value, DieSides.D8.value
        )
        val sampleDie = sampleDie.d20
        every { grove.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D20.value) } returns sampleDie

        // Act
        val result = SUT(credits)

        // Assert
        assertEquals(sampleDie, result)
    }
    
    @Test
    fun invoke_withMixedCredits_usesTotalPips() {
        // Arrange
        val die = mockk<dugsolutions.leaf.components.die.Die>(relaxed = true)
        every { die.value } returns 6
        
        val credits = Credits(mutableListOf(
            Credit.CredDie(die),
            Credit.CredAddToTotal(4)
        ))
        
        val availableDieSides = listOf(DieSides.D4.value, DieSides.D10.value, DieSides.D8.value)
        val sampleDie = sampleDie.d10
        every { grove.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D10.value) } returns sampleDie

        // Act
        val result = SUT(credits)

        // Assert
        assertEquals(sampleDie, result)
    }
} 
