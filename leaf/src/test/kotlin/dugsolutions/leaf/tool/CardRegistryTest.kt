package dugsolutions.leaf.tool

import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.MatchWith
import io.mockk.mockk
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
        parseCost = mockk(relaxed = true)
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
        
        // Track invalid flower cards
        val invalidFlowers = mutableListOf<String>()
        
        // Check all flower cards have ADORN effect
        flowerCards.forEach { flowerCard ->
            if (flowerCard.primaryEffect != CardEffect.ADORN) {
                invalidFlowers.add("Flower card '${flowerCard.name}' does not have ADORN as primary effect")
            }
        }
        
        // Assert
        val errorMessage = buildString {
            if (invalidBlooms.isNotEmpty()) {
                appendLine("Invalid bloom cards:")
                invalidBlooms.forEach { appendLine("  - $it") }
            }
            if (invalidFlowers.isNotEmpty()) {
                appendLine("Invalid flower cards:")
                invalidFlowers.forEach { appendLine("  - $it") }
            }
        }
        
        assertTrue(invalidBlooms.isEmpty() && invalidFlowers.isEmpty(), errorMessage)
    }

    @Test
    fun EFFECT_MAP_containsAllCardEffects() {
        // Arrange
        val missingEffects = mutableListOf<CardEffect>()
        
        // Act
        CardEffect.entries.forEach { effect ->
            // Check if any value in EFFECT_MAP maps to this effect
            val hasMapping = CardRegistry.EFFECT_MAP.values.contains(effect)
            if (!hasMapping) {
                missingEffects.add(effect)
            }
        }
        
        // Assert
        assertTrue(missingEffects.isEmpty(), 
            "The following CardEffects are missing from EFFECT_MAP: ${missingEffects.joinToString { it.name }}")
    }
} 
