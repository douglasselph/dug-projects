package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.game.purchase.domain.Credit
import dugsolutions.leaf.game.purchase.domain.Credits
import dugsolutions.leaf.market.Market
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectBestDieTest {

    private lateinit var selectBestDie: SelectBestDie
    private lateinit var market: Market
    private lateinit var dieFactory: DieFactory
    private val sampleDie = SampleDie()
    private val dieCost = DieCost()

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        market = mockk(relaxed = true)
        dieFactory = mockk(relaxed = true)

        // Create SelectBestDie instance
        selectBestDie = SelectBestDie(market, dieFactory, dieCost)
    }

    @Test
    fun invoke_whenAffordableHighSidedDieAvailable_returnsHighestAffordableDie() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(8)))
        val sampleDie = sampleDie.d8
        val availableDieSides = listOf(DieSides.D4.value, DieSides.D8.value, DieSides.D6.value, DieSides.D12.value)
        every { market.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D8.value) } returns sampleDie

        // Act
        val result = selectBestDie(credits)

        // Assert
        assertEquals(sampleDie, result)
    }

    @Test
    fun invoke_whenOnlyOneDieAffordable_returnsThatDie() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(6)))
        val sampleDie = sampleDie.d6
        val availableDieSides = listOf(DieSides.D6.value, DieSides.D8.value, DieSides.D10.value)
        every { market.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D6.value) } returns sampleDie

        // Act
        val result = selectBestDie(credits)

        // Assert
        assertEquals(sampleDie, result)
    }

    @Test
    fun invoke_whenNoDiceAffordable_returnsNull() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(3)))
        val availableDieSides = listOf(DieSides.D4.value, DieSides.D6.value, DieSides.D8.value)
        every { market.getAvailableDiceSides() } returns availableDieSides

        // Act
        val result = selectBestDie(credits)

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenNoDiceAvailable_returnsNull() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(10)))
        every { market.getAvailableDiceSides() } returns emptyList()

        // Act
        val result = selectBestDie(credits)

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
        every { market.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D10.value) } returns sampleDie

        // Act
        val result = selectBestDie(credits)

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
        every { market.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D20.value) } returns sampleDie

        // Act
        val result = selectBestDie(credits)

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
        every { market.getAvailableDiceSides() } returns availableDieSides
        every { dieFactory(DieSides.D10.value) } returns sampleDie

        // Act
        val result = selectBestDie(credits)

        // Assert
        assertEquals(sampleDie, result)
    }
} 
