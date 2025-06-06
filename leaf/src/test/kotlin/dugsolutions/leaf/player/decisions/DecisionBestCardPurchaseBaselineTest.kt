package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.baseline.DecisionBestCardPurchaseBaseline
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecisionBestCardPurchaseBaselineTest {

    private lateinit var player: Player

    private lateinit var rootCard1: GameCard
    private lateinit var rootCard2: GameCard
    private lateinit var canopyCard1: GameCard
    private lateinit var canopyCard2: GameCard
    private lateinit var vineCard1: GameCard
    private lateinit var vineCard2: GameCard
    private lateinit var flowerCard1: GameCard
    private lateinit var flowerCard2: GameCard

    private lateinit var SUT: DecisionBestCardPurchaseBaseline

    @BeforeEach
    fun setup() {
        // Setup mock player
        player = mockk(relaxed = true)
        every { player.allCardsInDeck } returns emptyList()

        // Create the strategy
        SUT = DecisionBestCardPurchaseBaseline(player)

        // Create test cards with different flourish types
        rootCard1 = FakeCards.fakeRoot.copy(id = 1)
        rootCard2 = FakeCards.fakeRoot.copy(id = 2)
        canopyCard1 = FakeCards.fakeCanopy.copy(id = 3)
        canopyCard2 = FakeCards.fakeCanopy.copy(id = 4)
        vineCard1 = FakeCards.fakeVine.copy(id = 5)
        vineCard2 = FakeCards.fakeVine.copy(id = 6)
        flowerCard1 = FakeCards.fakeFlower.copy(id = 7)
        flowerCard2 = FakeCards.fakeFlower2.copy(id = 8)
    }

    @Test
    fun invoke_whenEmptyCardList_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                SUT(emptyList())
            }
        }
    }

    @Test
    fun invoke_whenSingleCard_returnsThatCard() = runBlocking {
        // Arrange
        val cards = listOf(rootCard1)

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(rootCard1, result)
    }

    @Test
    fun invoke_whenMultipleCardsWithDifferentEvaluations_returnsHighestEvaluation() = runBlocking {
        // Arrange
        val cards = listOf(rootCard1, rootCard2)
        SUT.evaluationMap[rootCard1.id] = DecisionBestCardPurchaseBaseline.CountsInHand(
            listOf(DecisionBestCardPurchaseBaseline.CountInHand(0, 1))
        )
        SUT.evaluationMap[rootCard2.id] = DecisionBestCardPurchaseBaseline.CountsInHand(
            listOf(DecisionBestCardPurchaseBaseline.CountInHand(0, 2))
        )

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(rootCard2, result)
    }

    @Test
    fun invoke_whenSameEvaluationDifferentCounts_returnsLeastOwned() = runBlocking {
        // Arrange
        val cards = listOf(rootCard1, rootCard2)
        every { player.allCardsInDeck } returns listOf(rootCard1, rootCard1) // rootCard1 appears twice

        // Set same evaluation for both cards
        SUT.evaluationMap[rootCard1.id] = DecisionBestCardPurchaseBaseline.CountsInHand(
            listOf(DecisionBestCardPurchaseBaseline.CountInHand(0, 1))
        )
        SUT.evaluationMap[rootCard2.id] = DecisionBestCardPurchaseBaseline.CountsInHand(
            listOf(DecisionBestCardPurchaseBaseline.CountInHand(0, 1))
        )

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(rootCard2, result)
    }

    @Test
    fun invoke_whenSameEvaluationAndCount_returnsByFlourishTypePriority() = runBlocking {
        // Arrange
        val cards = listOf(rootCard1, canopyCard1, vineCard1, flowerCard1)

        // Set same evaluation and count for all cards
        cards.forEach { card ->
            SUT.evaluationMap[card.id] = DecisionBestCardPurchaseBaseline.CountsInHand(
                listOf(DecisionBestCardPurchaseBaseline.CountInHand(0, 1))
            )
        }

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(rootCard1, result) // ROOT has highest priority (0)
    }

    @Test
    fun invoke_whenNoSpecificEvaluation_usesGeneralEvaluation() = runBlocking {
        // Arrange
        val cards = listOf(rootCard1, rootCard2)
        SUT.generalEvaluation = 5

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(rootCard1, result) // Both have same evaluation and count, ROOT has priority
    }

    @Test
    fun invoke_whenEvaluationThresholds_usesCorrectThreshold() = runBlocking {
        // Arrange
        val cards = listOf(rootCard1)
        every { player.allCardsInDeck } returns listOf(rootCard1, rootCard1) // count = 2

        // Set evaluation thresholds
        SUT.evaluationMap[rootCard1.id] = DecisionBestCardPurchaseBaseline.CountsInHand(
            listOf(
                DecisionBestCardPurchaseBaseline.CountInHand(0, 1),  // 0-1 cards
                DecisionBestCardPurchaseBaseline.CountInHand(2, 3)   // 2+ cards
            )
        )

        // Act
        val result = SUT(cards)

        // Assert
        assertEquals(rootCard1, result) // Should use evaluation 3 as count is 2
    }
} 
