package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.FloralBonusCount
import dugsolutions.leaf.player.decisions.DecisionDirector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FlowerCardMatchValueTest {

    companion object {
        private const val FLOWER_CARD_ID = 1
        private const val BLOOM_CARD_ID = 2
    }

    private val mockFloralBonusCount: FloralBonusCount = mockk(relaxed = true)
    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockDecisionDirector: DecisionDirector = mockk(relaxed = true)
    private lateinit var mockBloomCard: GameCard
    private lateinit var mockFlowerCard: GameCard

    private val SUT: FlowerCardMatchValue = FlowerCardMatchValue(mockFloralBonusCount)

    @BeforeEach
    fun setup() {
        mockBloomCard = mockk {
            every { id } returns BLOOM_CARD_ID
            every { type } returns FlourishType.BLOOM
            every { matchWith } returns MatchWith.Flower(3)
            every { flowerCardId } returns FLOWER_CARD_ID
        }
        mockFlowerCard = mockk {
            every { id } returns FLOWER_CARD_ID
            every { type } returns FlourishType.FLOWER
        }
        every { mockPlayer.decisionDirector } returns mockDecisionDirector
    }

    @Test
    fun invoke_whenNotBloomCard_returnsZero() = runBlocking {
        // Arrange
        every { mockBloomCard.type } returns FlourishType.ROOT

        // Act
        val result = SUT(mockPlayer, mockBloomCard)

        // Assert
        assertEquals(0, result)
        coVerify(exactly = 0) { mockPlayer.decisionDirector.flowerSelectDecision() }
    }

    @Test
    fun invoke_whenNotMatchWithFlower_returnsZero() = runBlocking {
        // Arrange
        every { mockBloomCard.matchWith } returns MatchWith.OnRoll(6)

        // Act
        val result = SUT(mockPlayer, mockBloomCard)

        // Assert
        assertEquals(0, result)
        coVerify(exactly = 0) { mockPlayer.decisionDirector.flowerSelectDecision() }
    }

    @Test
    fun invoke_whenNoFlowerCardId_returnsZero() = runBlocking {
        // Arrange
        every { mockBloomCard.flowerCardId } returns null

        // Act
        val result = SUT(mockPlayer, mockBloomCard)

        // Assert
        assertEquals(0, result)
        coVerify(exactly = 0) { mockPlayer.decisionDirector.flowerSelectDecision() }
    }

    @Test
    fun invoke_whenNoFlowerCardsInHand_returnsZeroBonus() = runBlocking {
        // Arrange
        every { mockPlayer.cardsInHand } returns emptyList()
        coEvery { mockDecisionDirector.flowerSelectDecision() } returns emptyList()
        every { mockFloralBonusCount(any(), any()) } returns 0

        // Act
        val result = SUT(mockPlayer, mockBloomCard)

        // Assert
        assertEquals(0, result)
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify(exactly = 0) { mockPlayer.addCardToFloralArray(any()) }
    }

} 
