package dugsolutions.leaf.game.acquire.credit

import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieBase
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.game.acquire.domain.Adjusted
import dugsolutions.leaf.game.acquire.domain.Credit
import dugsolutions.leaf.game.acquire.domain.Credits
import dugsolutions.leaf.game.acquire.domain.totalValue
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombinationGeneratorTest {

    private lateinit var mockEffectToCredits: EffectToCredits
    private lateinit var mockPlayer: Player
    private lateinit var testDie1: Die
    private lateinit var testDie2: Die
    private lateinit var randomizer: RandomizerTD
    private lateinit var sampleDie: SampleDie
    
    private lateinit var SUT: CombinationGenerator

    @BeforeEach
    fun setup() {
        randomizer = RandomizerTD()
        mockEffectToCredits = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        sampleDie = SampleDie(randomizer)
        
        // Create test dice
        testDie1 = sampleDie.d6.adjustTo(3)
        testDie2 = sampleDie.d8.adjustTo(5)
        
        SUT = CombinationGenerator(mockEffectToCredits)
    }
    
    @Test
    fun invoke_withNoDiceOrAdjustments_returnsOnlyBaseCombination() {
        // Arrange
        val credits = Credits().apply {
            // Empty credits, no dice or adjustments
        }
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertEquals(1, result.list.size)
        val combination = result.list[0]
        assertEquals(0, combination.values.dice.size)
        assertEquals(0, combination.addToTotal)
        assertTrue(combination.adjusted.isEmpty())
    }
    
    @Test
    fun invoke_withOnlyAddToTotal_returnsOnlyBaseCombination() {
        // Arrange
        val addToTotal = 3
        val creditsList = mutableListOf<Credit>(Credit.CredAddToTotal(addToTotal))
        val credits = Credits(creditsList)
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertEquals(1, result.list.size)
        val combination = result.list[0]
        assertEquals(0, combination.values.dice.size)
        assertEquals(addToTotal, combination.addToTotal)
        assertTrue(combination.adjusted.isEmpty())
    }
    
    @Test
    fun invoke_withOnlyDice_returnsOnlyBaseCombination() {
        // Arrange
        val creditsList = mutableListOf<Credit>(
            Credit.CredDie(testDie1),
            Credit.CredDie(testDie2)
        )
        val credits = Credits(creditsList)
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        assertEquals(1, result.list.size)
        val combination = result.list[0]
        assertEquals(2, combination.values.dice.size)
        assertEquals(testDie1 as DieBase, combination.values.dice[0])
        assertEquals(testDie2 as DieBase, combination.values.dice[1])
        assertEquals(0, combination.addToTotal)
        assertTrue(combination.adjusted.isEmpty())
    }
    
    @Test
    fun invoke_withSingleDieAndAdjustment_generatesCorrectCombinations() {
        // Arrange
        val adjustment = 2
        val creditsList = mutableListOf<Credit>(
            Credit.CredDie(testDie1),
            Credit.CredAdjustDie(adjustment)
        )
        val credits = Credits(creditsList)
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        // Should generate: base + 1 adjustment combination
        assertEquals(2, result.list.size) 
        
        // Check base combination
        val baseCombination = result.list[0]
        assertEquals(1, baseCombination.values.dice.size)
        assertEquals(0, baseCombination.addToTotal)
        assertTrue(baseCombination.adjusted.isEmpty())
        
        // Check adjustment combination
        val adjustmentCombination = result.list[1]
        assertEquals(1, adjustmentCombination.values.dice.size)
        assertEquals(0, adjustmentCombination.addToTotal)
        assertEquals(1, adjustmentCombination.adjusted.size)
        val adjusted = adjustmentCombination.adjusted[0] as Adjusted.ByAmount
        assertEquals(adjustment, adjusted.amount)
        
        // Verify values are different (adjustment applied)
        val baseValue = baseCombination.totalValue
        val adjustedValue = adjustmentCombination.totalValue
        assertEquals(baseValue + adjustment, adjustedValue)
    }
    
    @Test
    fun invoke_withMultipleDiceAndAdjustments_generatesAllCombinations() {
        // Arrange
        val adjustment1 = 2
        val adjustment2 = 1
        val creditsList = mutableListOf<Credit>(
            Credit.CredDie(testDie1),
            Credit.CredDie(testDie2),
            Credit.CredAdjustDie(adjustment1),
            Credit.CredAdjustDie(adjustment2)
        )
        val credits = Credits(creditsList)
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        // Should have: base + (2 dice × 2 adjustments) + combinations of 2 adjustments on different dice
        assertTrue(result.list.size > 1) // Should have multiple combinations
        
        // Check base combination is present
        val baseCombination = result.list[0]
        assertEquals(2, baseCombination.values.dice.size)
        assertEquals(0, baseCombination.addToTotal)
        assertTrue(baseCombination.adjusted.isEmpty())
        
        // Verify we have combinations where each die is adjusted by each adjustment
        val hasDie1Adj1 = result.list.any { combination ->
            combination.adjusted.any { adjusted ->
                when (adjusted) {
                    is Adjusted.ByAmount -> adjusted.die.sides == testDie1.sides && adjusted.amount == adjustment1
                    else -> false
                }
            }
        }
        val hasDie1Adj2 = result.list.any { combination ->
            combination.adjusted.any { adjusted ->
                when (adjusted) {
                    is Adjusted.ByAmount -> adjusted.die.sides == testDie1.sides && adjusted.amount == adjustment2
                    else -> false
                }
            }
        }
        val hasDie2Adj1 = result.list.any { combination ->
            combination.adjusted.any { adjusted ->
                when (adjusted) {
                    is Adjusted.ByAmount -> adjusted.die.sides == testDie2.sides && adjusted.amount == adjustment1
                    else -> false
                }
            }
        }
        val hasDie2Adj2 = result.list.any { combination ->
            combination.adjusted.any { adjusted ->
                when (adjusted) {
                    is Adjusted.ByAmount -> adjusted.die.sides == testDie2.sides && adjusted.amount == adjustment2
                    else -> false
                }
            }
        }
        
        assertTrue(hasDie1Adj1, "Should have combination with die1 adjusted by $adjustment1")
        assertTrue(hasDie1Adj2, "Should have combination with die1 adjusted by $adjustment2")
        assertTrue(hasDie2Adj1, "Should have combination with die2 adjusted by $adjustment1")
        assertTrue(hasDie2Adj2, "Should have combination with die2 adjusted by $adjustment2")
        
        // Verify we have at least one combination with multiple adjustments
        val hasMultipleAdjustments = result.list.any { it.adjusted.size > 1 }
        assertTrue(hasMultipleAdjustments, "Should have at least one combination with multiple adjustments")
    }
    
    @Test
    fun invoke_withSetToMax_generatesCorrectCombinations() {
        // Arrange
        val creditsList = mutableListOf<Credit>(
            Credit.CredDie(testDie1),
            Credit.CredDie(testDie2),
            Credit.CredSetToMax
        )
        val credits = Credits(creditsList)
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        // Should have: base + 1 combination per die for setToMax
        assertEquals(3, result.list.size) 
        
        // Check base combination
        val baseCombination = result.list[0]
        assertEquals(2, baseCombination.values.dice.size)
        assertEquals(0, baseCombination.addToTotal)
        assertTrue(baseCombination.adjusted.isEmpty())
        
        // Verify we have combinations where each die is set to max
        val hasDie1Max = result.list.any { combination ->
            combination.adjusted.any { adjusted ->
                when (adjusted) {
                    is Adjusted.ToMax -> adjusted.die.sides == testDie1.sides
                    else -> false
                }
            }
        }
        val hasDie2Max = result.list.any { combination ->
            combination.adjusted.any { adjusted ->
                when (adjusted) {
                    is Adjusted.ToMax -> adjusted.die.sides == testDie2.sides
                    else -> false
                }
            }
        }
        
        assertTrue(hasDie1Max, "Should have combination with die1 set to max")
        assertTrue(hasDie2Max, "Should have combination with die2 set to max")
    }
    
    @Test
    fun invoke_withMultipleSetToMax_generatesAllCombinations() {
        // Arrange
        val creditsList = mutableListOf<Credit>(
            Credit.CredDie(testDie1),
            Credit.CredDie(testDie2),
            Credit.CredSetToMax,
            Credit.CredSetToMax
        )
        val credits = Credits(creditsList)
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        // Should have: base + single die combos + combinations of multiple dice
        assertTrue(result.list.size > 3) // Base + single die combos + multiple dice
        
        // Verify we have a combination where both dice are set to max
        val hasBothDiceMax = result.list.any { combination ->
            combination.adjusted.size == 2 &&
            combination.adjusted.all { it is Adjusted.ToMax }
        }
        
        assertTrue(hasBothDiceMax, "Should have a combination with both dice set to max")
    }
    
    @Test
    fun invoke_withMixedAdjustmentsAndSetToMax_generatesAllCombinations() {
        // Arrange
        val adjustment = 2
        val creditsList = mutableListOf<Credit>(
            Credit.CredDie(testDie1),
            Credit.CredDie(testDie2),
            Credit.CredAdjustDie(adjustment),
            Credit.CredSetToMax
        )
        val credits = Credits(creditsList)
        every { mockEffectToCredits(mockPlayer) } returns credits
        
        // Act
        val result = SUT(mockPlayer)
        
        // Assert
        // Should have: base + adjustments + setToMax combinations
        assertTrue(result.list.size > 1)
        
        // Check that we have both types of adjustments
        val hasAdjustment = result.list.any { combination ->
            combination.adjusted.any { it is Adjusted.ByAmount }
        }
        val hasSetToMax = result.list.any { combination ->
            combination.adjusted.any { it is Adjusted.ToMax }
        }
        
        assertTrue(hasAdjustment, "Should have combinations with ByAmount adjustments")
        assertTrue(hasSetToMax, "Should have combinations with ToMax adjustments")
    }
} 
