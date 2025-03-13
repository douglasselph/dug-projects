package dugsolutions.leaf.game.turn.select

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.FlourishType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectCardToRetainTest {

    private lateinit var SUT: SelectCardToRetain
    private lateinit var costScore: CostScore

    // Using FakeCards instead of mocks
    private val fakeSeedling = FakeCards.fakeSeedling       // SEEDLING type
    private val fakeRoot = FakeCards.fakeRoot               // ROOT type
    private val fakeRoot2 = FakeCards.fakeRoot2             // ROOT type with different cost
    private val fakeBloom = FakeCards.fakeBloom             // BLOOM type
    private val fakeBloom2 = FakeCards.fakeBloom2           // BLOOM type with different cost
    private val fakeVine = FakeCards.fakeVine               // VINE type
    private val fakeCanopy = FakeCards.fakeCanopy           // CANOPY type

    @BeforeEach
    fun setup() {
        costScore = mockk()
        SUT = SelectCardToRetain(costScore)

        // Configure cost scores for all cards
        every { costScore(fakeSeedling.cost) } returns 2
        every { costScore(fakeRoot.cost) } returns 3
        every { costScore(fakeRoot2.cost) } returns 3 
        every { costScore(fakeBloom.cost) } returns 6
        every { costScore(fakeBloom2.cost) } returns 8
        every { costScore(fakeVine.cost) } returns 5
        every { costScore(fakeCanopy.cost) } returns 7
    }

    @Test
    fun invoke_whenNoCards_returnsNull() {
        // Act
        val result = SUT(emptyList())

        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenSingleCard_returnsThatCard() {
        // Arrange
        val cards = listOf(fakeSeedling)

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(fakeSeedling.id, result?.id)
    }

    @Test
    fun invoke_whenMultipleCards_returnsBestFlourishType() {
        // Arrange
        val cards = listOf(fakeSeedling, fakeRoot, fakeBloom, fakeVine, fakeCanopy)

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(fakeBloom.id, result?.id) // BLOOM is highest priority flourish type
    }

    @Test
    fun invoke_whenMultipleCardsOfSameType_returnsHighestCostScore() {
        // Arrange
        val cards = listOf(fakeBloom, fakeBloom2)
        every { costScore(fakeBloom.cost) } returns 6
        every { costScore(fakeBloom2.cost) } returns 8
        val expectedCard = fakeBloom2

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(expectedCard.id, result?.id) // Higher cost score within BLOOM type
    }

    @Test
    fun invoke_whenFilteredByFlourishType_returnsOnlyMatchingType() {
        // Arrange
        val cards = listOf(fakeSeedling, fakeRoot, fakeBloom, fakeVine, fakeCanopy)

        // Act
        val result = SUT(cards, FlourishType.ROOT)

        // Assert
        assertEquals(fakeRoot.id, result?.id) // Should only consider ROOT types
    }

    @Test
    fun invoke_whenMultipleCardsOfRequestedType_returnsHighestCostScore() {
        // Arrange
        val cards = listOf(fakeRoot, fakeRoot2)
        // Both root cards have same cost score, should pick the first one
        
        // Act
        val result = SUT(cards, FlourishType.ROOT)

        // Assert
        assertEquals(fakeRoot.id, result?.id) // First ROOT card with same cost score
    }

    @Test
    fun invoke_whenNoCardsOfRequestedType_returnsNull() {
        // Arrange
        val cards = listOf(fakeSeedling, fakeRoot, fakeBloom)

        // Act
        val result = SUT(cards, FlourishType.CANOPY)

        // Assert
        assertNull(result)
    }
} 
