package dugsolutions.leaf.game.purchase.domain

import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.SampleDie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreditsTest {

    private lateinit var sampleDie: SampleDie
    
    @BeforeEach
    fun setup() {
        sampleDie = SampleDie()
    }
    
    @Test
    fun isNotEmpty_withEmptyList_returnsFalse() {
        // Arrange
        val credits = Credits()
        
        // Act
        val result = credits.isNotEmpty
        
        // Assert
        assertFalse(result, "Empty credits should return false for isNotEmpty")
    }
    
    @Test
    fun isNotEmpty_withNonEmptyList_returnsTrue() {
        // Arrange
        val credits = Credits(mutableListOf(Credit.CredAddToTotal(3)))
        
        // Act
        val result = credits.isNotEmpty
        
        // Assert
        assertTrue(result, "Non-empty credits should return true for isNotEmpty")
    }
    
    @Test
    fun size_withEmptyList_returnsZero() {
        // Arrange
        val credits = Credits()
        
        // Act
        val result = credits.size
        
        // Assert
        assertEquals(0, result, "Empty credits should have size of 0")
    }
    
    @Test
    fun size_withMultipleItems_returnsCorrectCount() {
        // Arrange
        val creditList = mutableListOf(
            Credit.CredAddToTotal(3),
            Credit.CredSetToMax,
            Credit.CredRerollDie
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.size
        
        // Assert
        assertEquals(3, result, "Credits size should match the number of items in the list")
    }
    
    @Test
    fun pipTotal_withEmptyList_returnsZero() {
        // Arrange
        val credits = Credits()
        
        // Act
        val result = credits.pipTotal
        
        // Assert
        assertEquals(0, result, "Empty credits should have pipTotal of 0")
    }
    
    @Test
    fun pipTotal_withDifferentCreditTypes_returnsCorrectSum() {
        // Arrange
        val die = sampleDie.d6 // Let's say it rolled a 3
        val dieValue = die.value
        
        val creditList = mutableListOf(
            Credit.CredDie(die),                // Should add die value
            Credit.CredAddToTotal(5),           // Should add 5
            Credit.CredAdjustDie(2),            // Should add 2 (positive adjustment)
            Credit.CredAdjustDie(-3),           // Should add 0 (negative adjustment is capped at 0)
            Credit.CredSetToMax,                // Should not contribute to pip total
            Credit.CredRerollDie,               // Should not contribute to pip total
            Credit.CredReduceCost(FlourishType.ROOT, 2) // Should not contribute to pip total
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.pipTotal
        
        // Assert
        assertEquals(dieValue + 5 + 2 + 0, result, "PipTotal should be the sum of die value, addToTotal and positive adjustments")
    }
    
    @Test
    fun addToTotal_withEmptyList_returnsZero() {
        // Arrange
        val credits = Credits()
        
        // Act
        val result = credits.addToTotal
        
        // Assert
        assertEquals(0, result, "Empty credits should have addToTotal of 0")
    }
    
    @Test
    fun addToTotal_withDifferentCreditTypes_onlyCountsAddToTotalCredits() {
        // Arrange
        val die = sampleDie.d6
        
        val creditList = mutableListOf(
            Credit.CredDie(die),                // Should not contribute
            Credit.CredAddToTotal(5),           // Should add 5
            Credit.CredAddToTotal(3),           // Should add 3
            Credit.CredAdjustDie(2),            // Should not contribute
            Credit.CredSetToMax,                // Should not contribute
            Credit.CredRerollDie,               // Should not contribute
            Credit.CredReduceCost(FlourishType.ROOT, 2) // Should not contribute
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.addToTotal
        
        // Assert
        assertEquals(8, result, "AddToTotal should only sum the amounts from CredAddToTotal credits")
    }
    
    @Test
    fun dieList_withEmptyList_returnsEmptyList() {
        // Arrange
        val credits = Credits()
        
        // Act
        val result = credits.dieList
        
        // Assert
        assertTrue(result.isEmpty(), "Empty credits should return empty dieList")
    }
    
    @Test
    fun dieList_withDifferentCreditTypes_onlyExtractsDice() {
        // Arrange
        val die1 = sampleDie.d6
        val die2 = sampleDie.d8
        
        val creditList = mutableListOf(
            Credit.CredDie(die1),               // Should be included
            Credit.CredDie(die2),               // Should be included
            Credit.CredAddToTotal(5),           // Should not be included
            Credit.CredAdjustDie(2),            // Should not be included
            Credit.CredSetToMax,                // Should not be included
            Credit.CredRerollDie,               // Should not be included
            Credit.CredReduceCost(FlourishType.ROOT, 2) // Should not be included
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.dieList
        
        // Assert
        assertEquals(2, result.size, "DieList should only contain dice from CredDie credits")
        assertTrue(result.contains(die1), "DieList should contain die1")
        assertTrue(result.contains(die2), "DieList should contain die2")
    }
    
    @Test
    fun adjustList_withEmptyList_returnsEmptyList() {
        // Arrange
        val credits = Credits()
        
        // Act
        val result = credits.adjustList
        
        // Assert
        assertTrue(result.isEmpty(), "Empty credits should return empty adjustList")
    }
    
    @Test
    fun adjustList_withDifferentCreditTypes_onlyExtractsAdjustments() {
        // Arrange
        val creditList = mutableListOf(
            Credit.CredDie(sampleDie.d6),        // Should not be included
            Credit.CredAddToTotal(5),            // Should not be included
            Credit.CredAdjustDie(2),             // Should be included
            Credit.CredAdjustDie(-1),            // Should be included
            Credit.CredSetToMax,                 // Should not be included
            Credit.CredRerollDie,                // Should not be included
            Credit.CredReduceCost(FlourishType.ROOT, 2) // Should not be included
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.adjustList
        
        // Assert
        assertEquals(2, result.size, "AdjustList should only contain adjustment values from CredAdjustDie credits")
        assertTrue(result.contains(2), "AdjustList should contain adjustment value 2")
        assertTrue(result.contains(-1), "AdjustList should contain adjustment value -1")
    }
    
    @Test
    fun numSetToMax_withEmptyList_returnsZero() {
        // Arrange
        val credits = Credits()
        
        // Act
        val result = credits.numSetToMax
        
        // Assert
        assertEquals(0, result, "Empty credits should have numSetToMax of 0")
    }
    
    @Test
    fun numSetToMax_withDifferentCreditTypes_onlyCountsSetToMax() {
        // Arrange
        val creditList = mutableListOf(
            Credit.CredDie(sampleDie.d6),        // Should not be counted
            Credit.CredAddToTotal(5),            // Should not be counted
            Credit.CredAdjustDie(2),             // Should not be counted
            Credit.CredSetToMax,                 // Should be counted
            Credit.CredSetToMax,                 // Should be counted
            Credit.CredRerollDie,                // Should not be counted
            Credit.CredReduceCost(FlourishType.ROOT, 2) // Should not be counted
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.numSetToMax
        
        // Assert
        assertEquals(2, result, "NumSetToMax should count the number of CredSetToMax credits")
    }
    
    @Test
    fun contains_withCreditInList_returnsTrue() {
        // Arrange
        val credit = Credit.CredAddToTotal(5)
        val creditList = mutableListOf(
            Credit.CredDie(sampleDie.d6),
            credit,
            Credit.CredSetToMax
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.contains(credit)
        
        // Assert
        assertTrue(result, "Contains should return true for a credit that is in the list")
    }
    
    @Test
    fun contains_withCreditNotInList_returnsFalse() {
        // Arrange
        val credit = Credit.CredAddToTotal(5)
        val creditList = mutableListOf(
            Credit.CredDie(sampleDie.d6),
            Credit.CredSetToMax
        )
        val credits = Credits(creditList)
        
        // Act
        val result = credits.contains(credit)
        
        // Assert
        assertFalse(result, "Contains should return false for a credit that is not in the list")
    }
} 