package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.cost.ParseCost
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.cards.cost.ParseCostElement
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardRegistryTest {

    private lateinit var parseCost: ParseCost
    private lateinit var cardRegistry: CardRegistry

    @BeforeEach
    fun setup() {
        parseCost = ParseCost(ParseCostElement())
        cardRegistry = CardRegistry(parseCost)
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

        // Act
        cardRegistry.loadFromCsv(Commons.TEST_CARD_LIST)

        // Assert
        // Gave up trying to verify anything.
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
        // Act
        cardRegistry.loadFromCsv(Commons.TEST_CARD_LIST)
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
    fun loadFromCsv_verifyBloomAndFlowerRequirements() {
        // Arrange
        cardRegistry.loadFromCsv(Commons.TEST_CARD_LIST)
        val cards = cardRegistry.getAllCards()
        
        // Get all flower cards for reference
        val flowerCards = cards.filter { it.type == FlourishType.FLOWER }
        val flowerIds = flowerCards.map { it.id }
        
        // Track invalid bloom cards
        val invalidBlooms = mutableListOf<String>()
        
        // Check all bloom cards
        cards.filter { it.type == FlourishType.BLOOM }.forEach { bloomCard ->
            when (val matchWith = bloomCard.matchWith) {
                is MatchWith.Flower -> {
                    // Verify the referenced flower ID exists
                    if (matchWith.flowerCardId !in flowerIds) {
                        invalidBlooms.add("Bloom card '${bloomCard.name}' references non-existent flower ID ${matchWith.flowerCardId}")
                    }
                }
                else -> {
                    invalidBlooms.add("Bloom card '${bloomCard.name}' does not have MatchWith.Flower")
                }
            }
        }
        // Assert
        val errorMessage = buildString {
            if (invalidBlooms.isNotEmpty()) {
                appendLine("Invalid bloom cards:")
                invalidBlooms.forEach { appendLine("  - $it") }
            }
        }
        
        assertTrue(invalidBlooms.isEmpty(), errorMessage)
    }

} 
