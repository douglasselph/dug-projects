package dugsolutions.leaf.cards

import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameCardsTest {

    private lateinit var randomizer: Randomizer
    private lateinit var gameCards: GameCards
    private lateinit var costScore: CostScore

    @BeforeEach
    fun setup() {
        // Create mock randomizer
        randomizer = mockk(relaxed = true)
        costScore = mockk(relaxed = true)

        // Create GameCards instance with FakeCards
        gameCards = GameCards(FakeCards.ALL_CARDS, randomizer, costScore)
    }

    @Test
    fun size_returnsCorrectNumberOfCards() {
        // Arrange
        val expectedSize = FakeCards.ALL_CARDS.size
        // Act
        val result = gameCards.size
        // Assert
        assertEquals(expectedSize, result)
    }

    @Test
    fun getByType_whenTypeExists_returnsCorrectCards() {
        // Arrange
        val expectedSize = FakeCards.ALL_ROOT.size
        // Act
        val result = gameCards.getByType(FlourishType.ROOT)

        // Assert
        assertEquals(expectedSize, result.size)
        assertEquals(FakeCards.fakeRoot, result[0])
        assertEquals(FakeCards.fakeRoot2, result[1])
    }

    @Test
    fun getByType_whenTypeDoesNotExist_returnsEmptyList() {
        // Arrange
        // Act
        val result = gameCards.getByType(FlourishType.NONE)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun plus_combinesTwoGameCards() {
        // Arrange
        val additionalCards = GameCards(FakeCards.ALL_BLOOM, randomizer, costScore)
        val expectedSize = gameCards.size + additionalCards.size

        // Act
        val result = gameCards + additionalCards

        // Assert
        assertEquals(expectedSize, result.size)
    }

    @Test
    fun getOrNull_whenIndexValid_returnsCard() {
        // Act
        val result = gameCards.getOrNull(10000)

        // Assert
        assertEquals(null, result)
    }

    @Test
    fun shuffled_returnsShuffledCards() {
        // Arrange
        val shuffledCards = FakeCards.ALL_ROOT + FakeCards.ALL_BLOOM
        every { randomizer.shuffled(any<List<GameCard>>()) } returns shuffledCards

        // Act
        val result = gameCards.shuffled()

        // Assert
        assertEquals(shuffledCards.size, result.size)
    }

} 