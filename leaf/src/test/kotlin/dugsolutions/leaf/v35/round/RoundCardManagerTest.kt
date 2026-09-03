package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RoundCardManagerTest {

    private lateinit var sourceCards: List<RoundCard>
    private lateinit var manager: RoundCardManager

    @BeforeEach
    fun setup() {
        val registry = RoundCardRegistry(GameEffectConverter())
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        sourceCards = registry.getAllCards()
        manager = RoundCardManager()
        manager.loadCards(sourceCards)
    }

    @Test
    fun loadCards_whenRegistryProvided_loadsAllCards() {
        // Arrange
        val registry = RoundCardRegistry(GameEffectConverter())
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))
        val result = RoundCardManager()

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
        assertEquals(12, result)
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
        val result = manager.getCard("Not_A_Round_Card")

        // Assert
        assertNull(result)
    }

    @Test
    fun getCardsByType_returnsOnlyBattleDefinitions() {
        // Act
        val result = manager.getCardsByType(RoundCardType.BATTLE)

        // Assert
        assertEquals(6, result.size)
        assertEquals(
            sourceCards.filter { it.type == RoundCardType.BATTLE },
            result
        )
    }

    @Test
    fun getRoundCardsByType_returnsOnlyCultivationDefinitions() {
        // Act
        val result = manager.getRoundCardsByType(RoundCardType.CULTIVATION)

        // Assert
        assertEquals(6, result.size)
        assertEquals(
            sourceCards.filter { it.type == RoundCardType.CULTIVATION },
            result.cards
        )
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
