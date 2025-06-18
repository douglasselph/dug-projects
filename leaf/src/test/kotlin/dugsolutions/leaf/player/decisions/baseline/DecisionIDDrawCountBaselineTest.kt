package dugsolutions.leaf.player.decisions.baseline

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionDrawCount
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DecisionIDDrawCountBaselineTest {

    companion object {
        private const val CARD_ID_1 = 1
    }

    private lateinit var mockPlayer: Player
    private lateinit var sampleCard1: GameCard
    private lateinit var sampleCard2: GameCard

    private lateinit var SUT: DecisionDrawCount

    @BeforeEach
    fun setup() {
        // Create mock dependencies
        mockPlayer = mockk(relaxed = true)

        SUT = DecisionDrawCountBaseline()

        sampleCard1 = FakeCards.fakeCanopy
        sampleCard2 = FakeCards.fakeVine
    }

    @Test
    fun invoke_whenNoDiceAndEmptyHand_returnsFullHandSize() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 10
        every { mockPlayer.diceInSupplyCount } returns 0
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(Commons.HAND_SIZE, result.count) // Commons.HAND_SIZE
    }

    @Test
    fun invoke_whenNoDiceAndPartialHand_returnsRemainingHandSize() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 10
        every { mockPlayer.diceInSupplyCount } returns 0
        every { mockPlayer.cardsInHand } returns listOf(sampleCard1, sampleCard2)

        // Act
        val cardCount = SUT(mockPlayer).count

        // Assert
        assertEquals(Commons.HAND_SIZE - 2, cardCount)
    }

    @Test
    fun invoke_whenNoCardsAndEmptyHand_returnsZero() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 0
        every { mockPlayer.diceInSupplyCount } returns 10
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(0, result.count)
    }

    @Test
    fun invoke_whenEqualSupplyAndEmptyHand_returnsHalfHandSize() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 5
        every { mockPlayer.diceInSupplyCount } returns 5
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(2, result.count) // Commons.HAND_SIZE / 2
    }

    @Test
    fun invoke_whenEqualSupplyAndPartialHand_returnsRemainingHalfHandSize() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 5
        every { mockPlayer.diceInSupplyCount } returns 5
        every { mockPlayer.cardsInHand } returns listOf(mockk { every { id } returns CARD_ID_1 })

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.count) // (Commons.HAND_SIZE / 2) - 1
    }

    @Test
    fun invoke_whenMoreCardsThanDiceAndEmptyHand_returnsHandSizeMinusOne() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 10
        every { mockPlayer.diceInSupplyCount } returns 5
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(Commons.HAND_SIZE - 1, result.count)
    }

    @Test
    fun invoke_whenMoreCardsThanDiceAndPartialHand_andHasCardInHand_returnsRemainingHandSizeMinusTwo() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 10
        every { mockPlayer.diceInSupplyCount } returns 5
        every { mockPlayer.cardsInHand } returns listOf(sampleCard1)

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(Commons.HAND_SIZE - 2, result.count)
    }

    @Test
    fun invoke_whenMoreDiceThanCardsAndEmptyHand_returnsOne() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 3
        every { mockPlayer.diceInSupplyCount } returns 10
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.count)
    }

    @Test
    fun invoke_whenMoreDiceThanCardsAndFullHand_returnsZero() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 3
        every { mockPlayer.diceInSupplyCount } returns 10
        every { mockPlayer.cardsInHand } returns List(5) { sampleCard1 }

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(0, result.count)
    }

    @Test
    fun invoke_whenLowSupplyAndNoCompost_returnsOne() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.diceInSupplyCount } returns 2
        every { mockPlayer.cardsInBedCount } returns 0
        every { mockPlayer.diceInBedCount } returns 0
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.count)
    }

    @Test
    fun invoke_whenLowSupplyWithCompost_considersCompostForDecision() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.diceInSupplyCount } returns 2
        every { mockPlayer.cardsInBedCount } returns 3
        every { mockPlayer.diceInBedCount } returns 1
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(Commons.HAND_SIZE - 1, result.count) // More cards (4) than dice (3) after including compost
    }

    @Test
    fun invoke_whenLowSupplyWithEqualCompost_returnsHalfHandSize() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.cardsInBedCount } returns 2
        every { mockPlayer.diceInBedCount } returns 2
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(Commons.HAND_SIZE / 2, result.count) // Equal cards (3) and dice (3) after including compost
    }

    @Test
    fun invoke_whenLowSupplyWithMoreDiceInCompost_returnsOne() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.cardsInBedCount } returns 1
        every { mockPlayer.diceInBedCount } returns 3
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.count) // More dice (4) than cards (2) after including compost
    }

    @Test
    fun invoke_whenLowSupplyWithNoCardsInCompost_returnsZero() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 0
        every { mockPlayer.diceInSupplyCount } returns 2
        every { mockPlayer.cardsInBedCount } returns 0
        every { mockPlayer.diceInBedCount } returns 3
        every { mockPlayer.cardsInHand } returns emptyList()

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(0, result.count) // No cards available even with compost
    }

    @Test
    fun invoke_whenLowSupplyWithPartialHand_considersCompostAndHandSize() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.cardsInBedCount } returns 3
        every { mockPlayer.diceInBedCount } returns 1
        every { mockPlayer.cardsInHand } returns listOf(sampleCard1, sampleCard2)

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(Commons.HAND_SIZE - 3, result.count) // More cards than dice, but with 2 cards in hand
    }
} 
