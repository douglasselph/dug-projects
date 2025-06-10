package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.game.acquire.domain.Combinations
import dugsolutions.leaf.game.acquire.domain.FakeCombination
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.local.CanPurchaseCards
import dugsolutions.leaf.player.domain.AppliedEffect
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PossibleCardsTest {

    private val canPurchaseCards = mockk<CanPurchaseCards>(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val sampleEffectsList = mutableListOf<AppliedEffect>()

    private val SUT = PossibleCards(canPurchaseCards)

    @BeforeEach
    fun setup() {
        every { mockPlayer.delayedEffectList } returns sampleEffectsList
    }

    @Test
    fun invoke_whenNoCombinations_returnsEmptyList() {
        // Arrange
        val combinations = Combinations(emptyList())
        val marketCards = listOf(FakeCards.fakeRoot)

        // Act
        val result = SUT(mockPlayer, combinations, marketCards)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenNoMarketCards_returnsEmptyList() {
        // Arrange
        val combinations = Combinations(listOf(FakeCombination.combinationD6))

        // Act
        val result = SUT(mockPlayer, combinations, emptyList())

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun invoke_whenCardsAvailable_returnsSimplifiedChoices() {
        // Arrange
        val combinations = Combinations(listOf(FakeCombination.combinationD6, FakeCombination.combinationD8))
        
        val marketCards = listOf(FakeCards.fakeRoot, FakeCards.fakeRoot2)
        
        every { canPurchaseCards(marketCards, any(), FakeCombination.combinationD6, any()) } returns listOf(FakeCards.fakeRoot)
        every { canPurchaseCards(marketCards, any(), FakeCombination.combinationD8, any()) } returns listOf(FakeCards.fakeRoot2)

        // Act
        val result = SUT(mockPlayer, combinations, marketCards)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.card == FakeCards.fakeRoot })
        assertTrue(result.any { it.card == FakeCards.fakeRoot2 })
    }

    @Test
    fun invoke_whenMultipleChoicesForSameCard_returnsSimplifiedChoice() {
        // Arrange
        val combination1 = FakeCombination.combinationD6
        val combination2 = FakeCombination.combinationD8
        val combinations = Combinations(listOf(combination1, combination2))
        
        val marketCards = listOf(FakeCards.fakeRoot)
        
        every { canPurchaseCards(marketCards, any(), combination1, any()) } returns listOf(FakeCards.fakeRoot)
        every { canPurchaseCards(marketCards, any(), combination2, any()) } returns listOf(FakeCards.fakeRoot)

        // Act
        val result = SUT(mockPlayer, combinations, marketCards)

        // Assert
        assertEquals(1, result.size)
        assertEquals(FakeCards.fakeRoot, result[0].card)
    }

    @Test
    fun invoke_whenNoValidCards_returnsEmptyList() {
        // Arrange
        val combination = FakeCombination.combinationD10
        val combinations = Combinations(listOf(combination))
        
        val marketCards = listOf(FakeCards.fakeRoot)
        
        every { canPurchaseCards(marketCards, any(), combination, any()) } returns emptyList()

        // Act
        val result = SUT(mockPlayer, combinations, marketCards)

        // Assert
        assertTrue(result.isEmpty())
    }
}
