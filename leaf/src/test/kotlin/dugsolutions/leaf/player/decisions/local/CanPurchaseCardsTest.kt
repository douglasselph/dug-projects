package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.DieValues
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.domain.AppliedEffect
import io.mockk.mockk
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanPurchaseCardsTest {

    companion object {
        private const val CARD_1_NAME = "Card 1"
        private const val CARD_2_NAME = "Card 2"
        private const val CARD_3_NAME = "Card 3"
    }

    private val sampleEffectsList = mutableListOf<AppliedEffect>()
    private val mockCanPurchaseCard = mockk<CanPurchaseCard>(relaxed = true)

    private val SUT: CanPurchaseCards = CanPurchaseCards(mockCanPurchaseCard)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_withEmptyMarketCards_returnsEmptyList() {
        // Arrange
        val marketCards = emptyList<GameCard>()
        val playerHasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(listOf(DieValue(6, 3)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result.isEmpty(), "Empty market cards should return empty result")
    }

    @Test
    fun invoke_withAllCardsAffordable_returnsAllCards() {
        // Arrange
        val card1 = createGameCard(CARD_1_NAME)
        val card2 = createGameCard(CARD_2_NAME)
        val card3 = createGameCard(CARD_3_NAME)
        val marketCards = listOf(card1, card2, card3)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        every { mockCanPurchaseCard(any(), any(), any(), any()) } returns true

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertEquals(3, result.size, "All cards should be returned when all are affordable")
        assertEquals(listOf(card1, card2, card3), result)
    }

    @Test
    fun invoke_withNoCardsAffordable_returnsEmptyList() {
        // Arrange
        val card1 = createGameCard(CARD_1_NAME)
        val card2 = createGameCard(CARD_2_NAME)
        val marketCards = listOf(card1, card2)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        every { mockCanPurchaseCard(any(), any(), any(), any()) } returns false

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result.isEmpty(), "No cards should be returned when none are affordable")
    }

    @Test
    fun invoke_withSomeCardsAffordable_returnsOnlyAffordableCards() {
        // Arrange
        val card1 = createGameCard(CARD_1_NAME)
        val card2 = createGameCard(CARD_2_NAME)
        val card3 = createGameCard(CARD_3_NAME)
        val marketCards = listOf(card1, card2, card3)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        every { mockCanPurchaseCard(card1, any(), any(), any()) } returns true
        every { mockCanPurchaseCard(card2, any(), any(), any()) } returns false
        every { mockCanPurchaseCard(card3, any(), any(), any()) } returns true

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertEquals(2, result.size, "Only affordable cards should be returned")
        assertEquals(listOf(card1, card3), result)
    }

    @Test
    fun invoke_passesCorrectParametersToCanPurchaseCard() {
        // Arrange
        val card = createGameCard(CARD_1_NAME)
        val marketCards = listOf(card)
        val playerHasFlourishTypes = listOf(FlourishType.ROOT, FlourishType.CANOPY)
        val dieValues = DieValues(listOf(DieValue(6, 4), DieValue(8, 3)))
        val combination = Combination(dieValues, 2)

        every { mockCanPurchaseCard(card, playerHasFlourishTypes, combination, sampleEffectsList) } returns true

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertEquals(1, result.size, "Card should be returned when CanPurchaseCard returns true")
        assertEquals(card, result[0])
    }

    @Test
    fun invoke_maintainsOrderOfAffordableCards() {
        // Arrange
        val card1 = createGameCard(CARD_1_NAME)
        val card2 = createGameCard(CARD_2_NAME)
        val card3 = createGameCard(CARD_3_NAME)
        val marketCards = listOf(card1, card2, card3)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        every { mockCanPurchaseCard(card1, any(), any(), any()) } returns false
        every { mockCanPurchaseCard(card2, any(), any(), any()) } returns true
        every { mockCanPurchaseCard(card3, any(), any(), any()) } returns true

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertEquals(2, result.size, "Two cards should be returned")
        assertEquals(card2, result[0], "First affordable card should be first in result")
        assertEquals(card3, result[1], "Second affordable card should be second in result")
    }

    private fun createGameCard(name: String): GameCard {
        return GameCard(
            id = 1,
            name = name,
            type = FlourishType.ROOT,
            resilience = 8,
            nutrient = 1,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.ADD_TO_TOTAL,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )
    }
} 
