package dugsolutions.leaf.v35.wisp

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WispCardManagerTest {

    private lateinit var sourceCards: List<WispCard>
    private lateinit var manager: WispCardManager

    @BeforeEach
    fun setup() {
        val registry = WispCardRegistry(GameEffectConverter())
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        sourceCards = registry.getAllCards()
        manager = WispCardManager()
        manager.loadCards(sourceCards)
    }

    @Test
    fun loadCards_whenRegistryProvided_loadsAllCards() {
        // Arrange
        val registry = WispCardRegistry(GameEffectConverter())
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))
        val result = WispCardManager()

        // Act
        result.loadCards(registry)

        // Assert
        assertEquals(registry.getAllCards(), result.getAllCards().cards)
    }

    @Test
    fun size_returnsNumberOfDefinitions() {
        // Act
        val result = manager.size

        // Assert
        assertEquals(13, result)
    }

    @Test
    fun getCard_whenNameExists_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = manager.getCard(expected.name)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCard_whenCaseAndWhitespaceDiffer_returnsCard() {
        // Arrange
        val expected = sourceCards.first()

        // Act
        val result = manager.getCard("  ${expected.name.uppercase()}  ")

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getCard_whenNameDoesNotExist_returnsNull() {
        // Act
        val result = manager.getCard("Not_A_Wisp")

        // Assert
        assertNull(result)
    }

    @Test
    fun getAllCards_returnsCardsInLoadOrder() {
        // Act
        val result = manager.getAllCards()

        // Assert
        assertEquals(sourceCards, result.cards)
    }

    @Test
    fun loadCards_whenCalledAgain_replacesExistingCards() {
        // Arrange
        val replacement = sourceCards.take(3)

        // Act
        manager.loadCards(replacement)

        // Assert
        assertEquals(replacement, manager.getAllCards().cards)
        assertEquals(3, manager.size)
    }

    @Test
    fun loadCards_whenDuplicateNamesProvided_throws() {
        // Arrange
        val card = sourceCards.first()

        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            manager.loadCards(listOf(card, card))
        }
    }

    @Test
    fun clear_removesAllCards() {
        // Act
        manager.clear()

        // Assert
        assertEquals(0, manager.size)
        assertEquals(emptyList(), manager.getAllCards().cards)
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()
}
