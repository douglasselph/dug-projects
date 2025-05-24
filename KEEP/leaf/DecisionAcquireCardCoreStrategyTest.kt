package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.game.turn.cost.CoverCost
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.game.turn.local.CardIsFree
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class DecisionAcquireCardCoreStrategyTest {

    private lateinit var cardIsFree: CardIsFree
    private lateinit var coverCost: CoverCost
    private lateinit var costScore: CostScore
    private lateinit var player: Player
    private lateinit var acquireCardDecision: DecisionAcquireCardCoreStrategy
    private lateinit var testDecision: TestDecisionAcquireCard
    
    // Different card types for testing type preference
    private val fakeSeedling = FakeCards.fakeSeedling  // SEEDLING type
    private val fakeRoot = FakeCards.fakeRoot         // ROOT type
    private val fakeBloom = FakeCards.fakeBloom       // BLOOM type
    private val fakeVine = FakeCards.fakeVine         // VINE type
    private val fakeCanopy = FakeCards.fakeCanopy     // CANOPY type
    
    // Cards of same type but different costs
    private val fakeBloom2 = FakeCards.fakeBloom2     // BLOOM type, higher cost

    @BeforeEach
    fun setup() {
        cardIsFree = mockk(relaxed = true)
        coverCost = mockk(relaxed = true)
        costScore = mockk(relaxed = true)
        player = mockk(relaxed = true)
        
        // Default behavior - no cards are free, but all are affordable
        every { cardIsFree(any(), player) } returns false
        every { coverCost(player, any()) } returns listOf(mockk<Die>())
        every { costScore(any<Cost>()) } returns 0

        // Set up cost scores (used for tiebreaking same type)
        every { costScore(fakeBloom.cost) } returns 10
        every { costScore(fakeBloom2.cost) } returns 15  // Higher cost score
        every { costScore(fakeVine.cost) } returns 4
        every { costScore(fakeCanopy.cost) } returns 5
        every { costScore(fakeRoot.cost) } returns 2
        every { costScore(fakeSeedling.cost) } returns 2
        
        acquireCardDecision = DecisionAcquireCardCoreStrategy(cardIsFree, coverCost, costScore)
        testDecision = TestDecisionAcquireCard()
    }

    @Test
    fun invoke_whenEmptyList_returnsNull() {
        // Act
        val result = acquireCardDecision(player, emptyList())
        // Assert
        assertNull(result)
    }

    @Test
    fun invoke_whenSingleCard_returnsThatCard() {
        // Arrange
        every { coverCost(player, fakeSeedling.cost) } returns listOf(mockk<Die>())
        
        // Act
        val result = acquireCardDecision(player, listOf(fakeSeedling))
        
        // Assert
        assertEquals(fakeSeedling, result?.card)
    }

    @Test
    fun invoke_whenCardUnderTestSpecified_returnsThatCard() {
        // Arrange
        acquireCardDecision.cardUnderTest = fakeVine.id
        val cards = listOf(
            fakeSeedling,
            fakeBloom,
            fakeVine
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeVine, result.card)
    }
    
    @Test
    fun invoke_whenCardUnderTestNotInList_followsNormalPriority() {
        // Arrange
        acquireCardDecision.cardUnderTest = 1000 // Not in list
        
        // Make a free card available (highest priority after card under test)
        every { cardIsFree(fakeRoot, player) } returns true
        
        val cards = listOf(
            fakeRoot,
            fakeBloom,
            fakeVine
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeRoot, result.card) // Should pick the free card
    }

    @Test
    fun invoke_whenFreeCardAvailable_selectsFreeCard() {
        // Arrange
        every { cardIsFree(fakeRoot, player) } returns true
        
        val cards = listOf(
            fakeRoot,  // Free
            fakeBloom, // Not free but higher type priority
            fakeVine   // Not free
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeRoot, result.card) // Free card trumps type priority
    }
    
    @Test
    fun invoke_whenMultipleFreeCardsAvailable_selectsByTypePreference() {
        // Arrange
        every { cardIsFree(fakeRoot, player) } returns true
        every { cardIsFree(fakeBloom, player) } returns true
        
        val cards = listOf(
            fakeRoot,  // Free, lower type priority
            fakeBloom  // Free, higher type priority
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeBloom, result.card) // BLOOM > ROOT in type preference
    }
    
    @Test
    fun invoke_whenMultipleCardsOfSameTypeAvailable_selectsHighestCostScore() {
        // Arrange
        every { cardIsFree(fakeBloom, player) } returns true
        every { cardIsFree(fakeBloom2, player) } returns true
        
        val cards = listOf(
            fakeBloom,  // Free, BLOOM type, lower cost score
            fakeBloom2  // Free, BLOOM type, higher cost score
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeBloom2, result.card) // Higher cost score within same type
    }
    
    @Test
    fun invoke_whenNoFreeCards_selectsAffordableCardsByTypePreference() {
        // Arrange
        // No free cards
        every { cardIsFree(any(), player) } returns false
        
        // All cards are affordable
        every { coverCost(player, any()) } returns listOf(mockk<Die>())
        
        val cards = listOf(
            fakeRoot,    // ROOT type (lowest priority)
            fakeCanopy,  // CANOPY type (medium priority)
            fakeBloom    // BLOOM type (highest priority)
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeBloom, result.card) // BLOOM has highest type priority
    }
    
    @Test
    fun invoke_whenTypeOrderingMatters_followsCorrectOrder() {
        // Arrange
        // No free cards
        every { cardIsFree(any(), player) } returns false
        
        // All cards are affordable
        every { coverCost(player, any()) } returns listOf(mockk<Die>())
        
        val cards = listOf(
            fakeRoot,    // ROOT (lowest)
            fakeCanopy,  // CANOPY (3rd)
            fakeVine,    // VINE (2nd)
            fakeBloom    // BLOOM (highest)
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeBloom, result.card) // BLOOM > VINE > CANOPY > ROOT
        
        // Remove BLOOM and test again
        val cardsWithoutBloom = listOf(fakeRoot, fakeCanopy, fakeVine)
        val resultWithoutBloom = acquireCardDecision(player, cardsWithoutBloom)
        assertEquals(fakeVine, resultWithoutBloom?.card) // VINE > CANOPY > ROOT
        
        // Remove VINE and test again
        val cardsWithoutVine = listOf(fakeRoot, fakeCanopy)
        val resultWithoutVine = acquireCardDecision(player, cardsWithoutVine)
        assertEquals(fakeCanopy, resultWithoutVine?.card) // CANOPY > ROOT
    }
    
    @Test
    fun invoke_whenSomeCardsNotAffordable_filtersThemOut() {
        // Arrange
        // No cards are free
        every { cardIsFree(any(), player) } returns false
        
        // Only some cards are affordable
        every { coverCost(player, fakeBloom.cost) } returns emptyList() // Not affordable
        every { coverCost(player, fakeVine.cost) } returns listOf(mockk<Die>()) // Affordable
        every { coverCost(player, fakeRoot.cost) } returns listOf(mockk<Die>()) // Affordable
        
        val cards = listOf(
            fakeRoot,  // Affordable
            fakeBloom, // Not affordable (higher type priority but can't afford)
            fakeVine   // Affordable (should be selected due to type priority)
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNotNull(result)
        assertEquals(fakeVine, result.card) // VINE > ROOT in affordable cards
    }
    
    @Test
    fun invoke_whenNoAffordableCards_returnsNull() {
        // Arrange
        // No cards are free
        every { cardIsFree(any(), player) } returns false
        
        // No cards are affordable
        every { coverCost(player, any()) } returns emptyList()
        
        val cards = listOf(
            fakeRoot,
            fakeBloom,
            fakeVine
        )
        
        // Act
        val result = acquireCardDecision(player, cards)
        
        // Assert
        assertNull(result) // No affordable cards
    }

    /**
     * Test implementation of DecisionAcquireCard for testing purposes.
     */
    private class TestDecisionAcquireCard : DecisionAcquireCard {
        var cardToReturn: GameCard? = null
        var returnNull: Boolean = false
        
        override operator fun invoke(player: Player, possibleCards: List<GameCard>): DecisionAcquireCard.Result? {
            if (possibleCards.isEmpty() || returnNull) {
                return null
            }
            val card = cardToReturn ?: possibleCards.first()
            return DecisionAcquireCard.Result(card)
        }
    }
} 