package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.turn.select.SelectPossibleDice
import dugsolutions.leaf.player.decisions.local.AcquireDieEvaluator
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AcquireDieEvaluatorTest {

    private lateinit var mockSelectPossibleDice: SelectPossibleDice
    private lateinit var mockEvaluateBestDiePurchase: EvaluateBestDiePurchase
    private lateinit var SUT: AcquireDieEvaluator

    private lateinit var sampleDie: SampleDie
    
    @BeforeEach
    fun setup() {
        mockSelectPossibleDice = mockk(relaxed = true)
        mockEvaluateBestDiePurchase = mockk(relaxed = true)
        SUT = AcquireDieEvaluator(mockSelectPossibleDice, mockEvaluateBestDiePurchase)
        sampleDie = SampleDie()
    }
    
    @Test
    fun invoke_withEmptyMarketDice_returnsNull() {
        // Arrange
        every { mockSelectPossibleDice() } returns emptyList()
        val combination = Combination(DieValues(listOf(DieValue(6, 3))), 0)
        val combinations = createCombinations(listOf(combination))
        every { mockEvaluateBestDiePurchase(any(), any()) } returns null

        // Act
        val result = SUT(combinations)
        
        // Assert
        assertNull(result, "Empty market dice should return null")
    }
    
    @Test
    fun invoke_whenNoCombinationCanAffordDie_returnsNull() {
        // Arrange
        val d6 = sampleDie.d6
        val d8 = sampleDie.d8
        every { mockSelectPossibleDice() } returns listOf(d6, d8)
        
        val combination1 = Combination(DieValues(listOf(DieValue(4, 2))), 0)
        val combination2 = Combination(DieValues(listOf(DieValue(6, 3))), 0)
        val combinations = createCombinations(listOf(combination1, combination2))
        
        // No dice can be afforded with any combination
        every { mockEvaluateBestDiePurchase(any(), combination1) } returns null
        every { mockEvaluateBestDiePurchase(any(), combination2) } returns null
        
        // Act
        val result = SUT(combinations)
        
        // Assert
        assertNull(result, "Should return null when no dice can be purchased")
    }
    
    @Test
    fun invoke_withOneCombinationOnePossibleDie_returnsDie() {
        // Arrange
        val d6 = sampleDie.d6
        every { mockSelectPossibleDice() } returns listOf(d6)
        
        val combination = Combination(DieValues(listOf(DieValue(6, 6))), 0)
        val combinations = createCombinations(listOf(combination))
        
        every { mockEvaluateBestDiePurchase(listOf(d6), combination) } returns d6
        
        // Act
        val result = SUT(combinations)
        
        // Assert
        assertEquals(d6, result?.die, "Should return the available die")
        assertEquals(combination, result?.combination, "Should return the combination used")
    }
    
    @Test
    fun invoke_withMultipleCombinations_selectsHighestSidedDie() {
        // Arrange
        val d6 = sampleDie.d6
        val d8 = sampleDie.d8
        val d10 = sampleDie.d10
        every { mockSelectPossibleDice() } returns listOf(d6, d8, d10)
        
        val combination1 = Combination(DieValues(listOf(DieValue(6, 6))), 0)
        val combination2 = Combination(DieValues(listOf(DieValue(8, 8))), 0)
        val combination3 = Combination(DieValues(listOf(DieValue(10, 10))), 0)
        val combinations = createCombinations(listOf(combination1, combination2, combination3))
        
        // Each combination can afford different dice
        every { mockEvaluateBestDiePurchase(any(), combination1) } returns d6
        every { mockEvaluateBestDiePurchase(any(), combination2) } returns d8
        every { mockEvaluateBestDiePurchase(any(), combination3) } returns d10
        
        // Act
        val result = SUT(combinations)
        
        // Assert
        assertEquals(d10, result?.die, "Should select the die with the most sides (d10)")
        assertEquals(combination3, result?.combination, "Should return the combination used for d10")
    }
    
    @Test
    fun invoke_withEqualSidedDice_selectsSmallestCombination() {
        // Arrange
        val d8a = sampleDie.d8
        val d8b = sampleDie.d8 // Another d8
        every { mockSelectPossibleDice() } returns listOf(d8a, d8b)
        
        // First combination uses fewer dice
        val combination1 = Combination(DieValues(listOf(DieValue(8, 8))), 0) // One die
        val combination2 = Combination(DieValues(listOf(DieValue(4, 4), DieValue(4, 4))), 0) // Two dice
        val combinations = createCombinations(listOf(combination1, combination2))
        
        // Both combinations can afford d8
        every { mockEvaluateBestDiePurchase(any(), combination1) } returns d8a
        every { mockEvaluateBestDiePurchase(any(), combination2) } returns d8b
        
        // Act
        val result = SUT(combinations)
        
        // Assert
        assertEquals(8, result?.die?.sides, "Should select a d8")
        assertEquals(combination1, result?.combination, "Should prefer the combination with fewer dice")
    }
    
    @Test
    fun invoke_withMultipleCombinationsSameDieCount_selectsFirst() {
        // Arrange
        val d8a = sampleDie.d8
        val d8b = sampleDie.d8 // Another d8
        every { mockSelectPossibleDice() } returns listOf(d8a, d8b)
        
        // Both combinations use the same number of dice
        val combination1 = Combination(DieValues(listOf(DieValue(6, 6))), 2) // One die + 2 addToTotal
        val combination2 = Combination(DieValues(listOf(DieValue(6, 6))), 2) // One die + 2 addToTotal
        val combinations = createCombinations(listOf(combination1, combination2))
        
        // Both combinations can afford d8
        every { mockEvaluateBestDiePurchase(any(), combination1) } returns d8a
        every { mockEvaluateBestDiePurchase(any(), combination2) } returns d8b
        
        // Act
        val result = SUT(combinations)
        
        // Assert
        assertEquals(8, result?.die?.sides, "Should select a d8")
        // When number of dice is equal, the first combination is selected by default
    }
    
    private fun createCombinations(combinations: List<Combination>): Combinations {
        return Combinations(combinations)
    }
} 
