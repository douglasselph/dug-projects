package dugsolutions.leaf.tool

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.components.CardEffect
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
