package dugsolutions.leaf.game.turn.local

import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.acquire.credit.CombinationGenerator
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EvaluateSimpleCostTest {

    // Test subject
    private lateinit var SUT: EvaluateSimpleCost

    // Dependencies
    private lateinit var mockCombinationGenerator: CombinationGenerator
    
    // Test data
    private lateinit var mockPlayer: Player
    private lateinit var testCombinations: MutableList<Combination>
    
    @BeforeEach
    fun setup() {
        // Arrange
        mockCombinationGenerator = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        
        // Create test combinations with known total values
        testCombinations = mutableListOf(
            createCombination(3), // totalValue = 3
            createCombination(5), // totalValue = 5
            createCombination(7), // totalValue = 7
            createCombination(10) // totalValue = 10
        )
        
        // Create the test subject
        SUT = EvaluateSimpleCost(mockCombinationGenerator)
    }

    private fun createCombination(totalValue: Int): Combination {
        // Choose the smallest die sides that can accommodate the total value
        val sides = when {
            totalValue <= 4 -> DieSides.D4
            totalValue <= 6 -> DieSides.D6
            totalValue <= 8 -> DieSides.D8
            totalValue <= 10 -> DieSides.D10
            totalValue <= 12 -> DieSides.D12
            else -> DieSides.D20
        }

        // Create a combination with the given total value (using dice and addToTotal)
        val diceValue = totalValue / 2
        val addToTotal = totalValue - diceValue

        return Combination(
            values = DieValues(listOf(DieValue(sides.value, diceValue))),
            addToTotal = addToTotal
        )
    }
    
    @Test
    fun invoke_whenNoCombinationsMeetAmount_returnsNull() {
        // Arrange
        val amount = 15
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(testCombinations)
        
        // Act
        val result = SUT(mockPlayer, amount)
        
        // Assert
        assertNull(result, "Should return null when no combinations meet the required amount")
    }
    
    @Test
    fun invoke_whenOneCombinationMeetsExactAmount_returnsThatCombination() {
        // Arrange
        val amount = 5
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(testCombinations)
        
        // Act
        val result = SUT(mockPlayer, amount)
        
        // Assert
        assertEquals(testCombinations[1], result, "Should return the combination that exactly matches the amount")
    }
    
    @Test
    fun invoke_whenMultipleCombinationsMeetAmount_returnsClosestOne() {
        // Arrange
        val amount = 6
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(testCombinations)
        
        // Act
        val result = SUT(mockPlayer, amount)
        
        // Assert
        assertEquals(testCombinations[2], result, "Should return the closest combination above the amount")
    }
    
    @Test
    fun invoke_whenAllCombinationsBelowAmount_returnsNull() {
        // Arrange
        val amount = 11
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(testCombinations)
        
        // Act
        val result = SUT(mockPlayer, amount)
        
        // Assert
        assertNull(result, "Should return null when all combinations are below the amount")
    }
    
    @Test
    fun invoke_withEmptyCombinations_returnsNull() {
        // Arrange
        val amount = 5
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(emptyList())
        
        // Act
        val result = SUT(mockPlayer, amount)
        
        // Assert
        assertNull(result, "Should return null when combinations list is empty")
    }
    
    @Test
    fun invoke_whenMultipleEquallyCloseCombinations_returnsLowest() {
        // Arrange
        val customCombinations = listOf(
            createCombination(5),
            createCombination(7), // Both this and next are 2 away from amount=9
            createCombination(11)
        )
        val costToAfford = 9
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(customCombinations)
        
        // Act
        val result = SUT(mockPlayer, costToAfford)
        
        // Assert
        assertEquals(customCombinations[2], result, "Should return the combination with the smallest difference that actually covers the cost")
    }
    
    @Test
    fun invoke_whenMultipleExactMatches_returnsFirst() {
        // Arrange
        val exactValue = 7
        val customCombinations = listOf(
            createCombination(5),
            createCombination(exactValue),
            createCombination(exactValue), // Duplicate exact match
            createCombination(10)
        )
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(customCombinations)
        
        // Act
        val result = SUT(mockPlayer, exactValue)
        
        // Assert
        assertEquals(customCombinations[1], result, "Should return the first exact match")
    }
    
    @Test
    fun invoke_whenAmountIsZero_returnsLowestCombination() {
        // Arrange
        val amount = 0
        every { mockCombinationGenerator(mockPlayer) } returns Combinations(testCombinations)
        
        // Act
        val result = SUT(mockPlayer, amount)
        
        // Assert
        assertEquals(testCombinations[0], result, "Should return the lowest combination when amount is 0")
    }
} 
