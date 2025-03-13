package dugsolutions.leaf.game.purchase.evaluator

import dugsolutions.leaf.components.DieCost
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.purchase.domain.Combination
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EvaluateBestDiePurchaseTest {

    private lateinit var dieCost: DieCost
    private lateinit var SUT: EvaluateBestDiePurchase
    private lateinit var sampleDie: SampleDie
    
    @BeforeEach
    fun setup() {
        dieCost = DieCost()
        SUT = EvaluateBestDiePurchase(dieCost)
        sampleDie = SampleDie()
    }
    
    @Test
    fun invoke_withEmptyMarketDice_returnsNull() {
        // Arrange
        val marketDice = emptyList<dugsolutions.leaf.components.die.Die>()
        val dieValues = DieValues(listOf(DieValue(6, 3)))
        val combination = Combination(dieValues, 0)
        
        // Act
        val result = SUT(marketDice, combination)
        
        // Assert
        assertNull(result, "Empty market dice should return null")
    }
    
    @Test
    fun invoke_withInsufficientValue_returnsNull() {
        // Arrange
        val marketDice = listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10)
        val dieValues = DieValues(listOf(DieValue(6, 2)))
        val combination = Combination(dieValues, 0) // Total value = 2
        
        // Act
        val result = SUT(marketDice, combination)
        
        // Assert
        assertNull(result, "Should return null when total value is insufficient for any die")
    }
    
    @Test
    fun invoke_withSufficientValueForSomeDice_returnsMaxCostAffordableDie() {
        // Arrange
        val marketDice = listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10, sampleDie.d12)
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 5)))
        val combination = Combination(dieValues, 0) // Total value = 8
        
        // Act
        val result = SUT(marketDice, combination)
        
        // Assert
        assertEquals(sampleDie.d8.sides, result?.sides, "Should return the most expensive die that can be afforded (d8)")
    }
    
    @Test
    fun invoke_withAddToTotal_includesAddToTotalInCalculation() {
        // Arrange
        val marketDice = listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10)
        val dieValues = DieValues(listOf(DieValue(6, 3)))
        val addToTotal = 5
        val combination = Combination(dieValues, addToTotal) // Total value = 8
        
        // Act
        val result = SUT(marketDice, combination)
        
        // Assert
        assertEquals(sampleDie.d8.sides, result?.sides, "Should include addToTotal in the calculation")
    }
    
    @Test
    fun invoke_withMultipleDiceCanAfford_returnsHighestSidedDie() {
        // Arrange
        val marketDice = listOf(sampleDie.d6, sampleDie.d8, sampleDie.d6) // Two d6 dice
        val dieValues = DieValues(listOf(DieValue(8, 8)))
        val combination = Combination(dieValues, 0) // Total value = 6
        
        // Act
        val result = SUT(marketDice, combination)
        
        // Assert
        assertEquals(sampleDie.d8.sides, result?.sides, "Should return the die with most sides when can afford multiple")
    }
    
    @Test
    fun invoke_withMultipleAffordableDice_returnsHighestCostDie() {
        // Arrange
        // Create a custom DieCost for this test only
        val d4 = sampleDie.d4
        val d6 = sampleDie.d6
        val d8 = sampleDie.d8
        val d10 = sampleDie.d10

        val marketDice = listOf(d4, d6, d8, d10) 
        val dieValues = DieValues(listOf(DieValue(12, 9)))
        val combination = Combination(dieValues, 0) // Total value = 9
        
        // Act
        val result = SUT(marketDice, combination)
        
        // Assert
        assertEquals(d8, result, "Should return the die with the highest cost that can be afforded")
    }
} 
