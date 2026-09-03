package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PlantCardManagerTest {

    private lateinit var sourceCards: List<PlantCard>
    private lateinit var manager: PlantCardManager

    @BeforeEach
    fun setup() {
        val registry = PlantCardRegistry()
        registry.loadFromCsv(
            dataPath(CardDataFiles.ROOT_CARD_LIST),
            dataPath(CardDataFiles.VF_CARD_LIST)
        )

        sourceCards = registry.getAllCards()
        manager = PlantCardManager()
        manager.loadCards(sourceCards)
    }

    @Test
    fun loadCards_whenRegistryProvided_loadsAllCards() {
        // Arrange
        val registry = PlantCardRegistry()
        registry.loadFromCsv(
            dataPath(CardDataFiles.ROOT_CARD_LIST),
            dataPath(CardDataFiles.VF_CARD_LIST)
        )
        val result = PlantCardManager()

        // Act
        result.loadCards(registry)

        // Assert
        assertEquals(registry.getAllCards(), result.getAllCards().cards)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        // Act
        val result = manager.size

        // Assert
        assertEquals(sourceCards.size, result)
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
        val result = manager.getCard("Not_A_Plant_Card")

        // Assert
        assertNull(result)
    }

    @Test
    fun getCardsByType_returnsMatchingCards() {
        // Arrange
        val expected = sourceCards.filter { it.type == PlantType.ROOT }

        // Act
        val result = manager.getCardsByType(PlantType.ROOT)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun getPlantCardsByType_returnsMatchingPlantCards() {
        // Arrange
        val expected = sourceCards.filter { it.type == PlantType.VINE }

        // Act
        val result = manager.getPlantCardsByType(PlantType.VINE)

        // Assert
        assertEquals(expected, result.cards)
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
