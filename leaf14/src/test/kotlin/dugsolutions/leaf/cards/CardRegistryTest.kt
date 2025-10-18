package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.common.Commons
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardRegistryTest {

    private lateinit var cardRegistry: CardRegistry

    @BeforeEach
    fun setup() {
        cardRegistry = CardRegistry()
    }

    @Test
    fun loadFromCsv_whenFileDoesNotExist_throwsException() {
        // Arrange - Using nonexistent file path
        val nonexistentFile = "nonexistent.csv"
        
        // Act
        // Assert
        assertThrows<IllegalArgumentException> {
            cardRegistry.loadFromCsv(nonexistentFile)
        }
    }

    @Test
    fun loadFromCsv_withTestFile_loadsAsExpected() {
        // Arrange
        val testCsvPath = Commons.CARD_LIST

        // Act
        cardRegistry.loadFromCsv(testCsvPath)

        // Assert
        // Basic verification that cards were loaded
        val allCards = cardRegistry.getAllCards()
        assertTrue(allCards.isNotEmpty(), "No cards were loaded from CSV")
    }

    @Test
    fun getCard_whenCardDoesNotExist_returnsNull() {
        // Arrange - Empty registry
        
        // Act
        val result = cardRegistry.getCard("NonExistentCard")
        
        // Assert
        assertNull(result)
    }

    @Test
    fun getAllCards_whenRegistryEmpty_returnsEmptyList() {
        // Arrange - Empty registry from setup
        
        // Act
        val result = cardRegistry.getAllCards()
        
        // Assert
        assertEquals(emptyList(), result)
    }
    
    @Test
    fun loadFromCsv_allCardsAreUnique() {
        // Arrange
        val testCsvPath = Commons.CARD_LIST
        
        // Act
        cardRegistry.loadFromCsv(testCsvPath)
        val cards = cardRegistry.getAllCards()

        // Assert
        
        // Get all IDs
        val ids = cards.map { it.id }
        
        // Check for duplicates
        val duplicates = ids.groupBy { it }
            .filter { it.value.size > 1 }
            .keys
        
        if (duplicates.isNotEmpty()) {
            println("Found duplicate card IDs: $duplicates")
            println("Cards with duplicate IDs:")
            duplicates.forEach { id ->
                val cardsWithId = cards.filter { it.id == id }
                println("ID $id:")
                cardsWithId.forEach { card ->
                    println("  - ${card.name}")
                }
            }
        }
        
        // Assert that there are no duplicates
        assertTrue(duplicates.isEmpty(), "Found duplicate card IDs: $duplicates")
    }

    @Test
    fun loadFromCsv_verifyCardProperties() {
        // Arrange
        val testCsvPath = "data/Cards-v14.csv"
        
        // Act
        cardRegistry.loadFromCsv(testCsvPath)
        val cards = cardRegistry.getAllCards()
        
        // Assert
        assertTrue(cards.isNotEmpty(), "No cards were loaded")
        
        // Verify that all cards have valid properties
        cards.forEach { card ->
            assertTrue(card.id > 0, "Card ${card.name} has invalid ID: ${card.id}")
            assertTrue(card.name.isNotEmpty(), "Card has empty name")
            assertTrue(card.resilience >= 0, "Card ${card.name} has negative resilience: ${card.resilience}")
            assertTrue(card.count > 0, "Card ${card.name} has invalid count: ${card.count}")
        }
    }

    @Test
    fun loadFromCsv_verifyFlourishTypes() {
        // Arrange
        val testCsvPath = "data/Cards-v14.csv"
        
        // Act
        cardRegistry.loadFromCsv(testCsvPath)
        val cards = cardRegistry.getAllCards()
        
        // Assert
        val validFlourishTypes = FlourishType.entries.toSet()
        val invalidCards = cards.filter { card ->
            !validFlourishTypes.contains(card.type)
        }
        
        if (invalidCards.isNotEmpty()) {
            println("Cards with invalid flourish types:")
            invalidCards.forEach { card ->
                println("  - ${card.name}: ${card.type}")
            }
        }
        
        assertTrue(invalidCards.isEmpty(), "Found cards with invalid flourish types")
    }

    @Test
    fun loadFromCsv_verifyMatchWithTypes() {
        // Arrange
        val testCsvPath = "data/Cards-v14.csv"
        
        // Act
        cardRegistry.loadFromCsv(testCsvPath)
        val cards = cardRegistry.getAllCards()
        
        // Assert
        val validMatchWithTypes = MatchWith.entries.toSet()
        val invalidCards = cards.filter { card ->
            !validMatchWithTypes.contains(card.matchWith)
        }
        
        if (invalidCards.isNotEmpty()) {
            println("Cards with invalid match with types:")
            invalidCards.forEach { card ->
                println("  - ${card.name}: ${card.matchWith}")
            }
        }
        
        assertTrue(invalidCards.isEmpty(), "Found cards with invalid match with types")
    }
}
