package dugsolutions.leaf.game.purchase.evaluator

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.purchase.domain.Combination
import dugsolutions.leaf.game.purchase.domain.Combinations
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDirector
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PurchaseCardEvaluatorTest {

    private lateinit var mockEvaluateCardPurchases: EvaluateCardPurchases
    private lateinit var mockPlayer: Player
    private lateinit var mockDecisionDirector: DecisionDirector
    private lateinit var SUT: PurchaseCardEvaluator
    
    @BeforeEach
    fun setup() {
        mockEvaluateCardPurchases = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockDecisionDirector = mockk(relaxed = true)
        
        every { mockPlayer.decisionDirector } returns mockDecisionDirector
        
        SUT = PurchaseCardEvaluator(mockEvaluateCardPurchases)
    }
    
    @Test
    fun invoke_withEmptyMarketCards_returnsNull() {
        // Arrange
        val marketCards = emptyList<GameCard>()
        val combinations = createCombinations(listOf(
            Combination(DieValues(listOf(DieValue(6, 3))), 0)
        ))
        every { mockPlayer.cardsInHand } returns emptyList()
        
        // Act
        val result = SUT(mockPlayer, combinations, marketCards)
        
        // Assert
        assertNull(result, "Empty market cards should return null")
    }
    
    @Test
    fun invoke_whenNoPossibleCards_returnsNull() {
        // Arrange
        val card = createGameCard("Test Card")
        val marketCards = listOf(card)
        val combination = Combination(DieValues(listOf(DieValue(6, 3))), 0)
        val combinations = createCombinations(listOf(combination))
        
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockEvaluateCardPurchases(marketCards, any(), combination) } returns emptyList()
        
        // Act
        val result = SUT(mockPlayer, combinations, marketCards)
        
        // Assert
        assertNull(result, "Should return null when no cards can be purchased")
    }
    
    @Test
    fun invoke_withOneCombinationOnePossibleCard_returnsCard() {
        // Arrange
        val card = createGameCard("Test Card")
        val marketCards = listOf(card)
        val combination = Combination(DieValues(listOf(DieValue(6, 3))), 0)
        val combinations = createCombinations(listOf(combination))
        
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockEvaluateCardPurchases(marketCards, any(), combination) } returns listOf(card)
        every { mockDecisionDirector.bestCardPurchase(listOf(card)) } returns card
        
        // Act
        val result = SUT(mockPlayer, combinations, marketCards)
        
        // Assert
        assertEquals(card, result?.card, "Should return the available card")
        assertEquals(combination, result?.combination, "Should return the combination used")
    }
    
    @Test
    fun invoke_withMultipleCombinations_returnsCardWithBestCombination() {
        // Arrange
        val card = createGameCard("Test Card")
        val marketCards = listOf(card)
        
        val combination1 = Combination(DieValues(listOf(DieValue(6, 3))), 0)
        val combination2 = Combination(DieValues(listOf(DieValue(8, 5))), 0)
        val combinations = createCombinations(listOf(combination1, combination2))
        
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockEvaluateCardPurchases(marketCards, any(), combination1) } returns listOf(card)
        every { mockEvaluateCardPurchases(marketCards, any(), combination2) } returns listOf(card)
        
        // When asked which card is best among identical options, return the first one
        val cardsCaptor = slot<List<GameCard>>()
        every { mockDecisionDirector.bestCardPurchase(capture(cardsCaptor)) } answers { cardsCaptor.captured[0] }
        
        // Act
        val result = SUT(mockPlayer, combinations, marketCards)
        
        // Assert
        assertEquals(card, result?.card, "Should return the best card")
        // The exact combination doesn't matter in this test since both can purchase the card
    }
    
    @Test
    fun invoke_withMultipleCards_selectsBestCardAccordingToDirector() {
        // Arrange
        val card1 = createGameCard("Card 1")
        val card2 = createGameCard("Card 2")
        val marketCards = listOf(card1, card2)
        
        val combination = Combination(DieValues(listOf(DieValue(6, 3))), 0)
        val combinations = createCombinations(listOf(combination))
        
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockEvaluateCardPurchases(marketCards, any(), combination) } returns listOf(card1, card2)
        every { mockDecisionDirector.bestCardPurchase(listOf(card1, card2)) } returns card2
        every { mockDecisionDirector.bestCardPurchase(listOf(card2)) } returns card2

        // Act
        val result = SUT(mockPlayer, combinations, marketCards)
        
        // Assert
        assertEquals(card2, result?.card, "Should return the card chosen by the decision director")
        assertEquals(combination, result?.combination, "Should return the combination used")
    }
    
    @Test
    fun invoke_withMultipleCombinationsForDifferentCards_selectsBestOverallCard() {
        // Arrange
        val card1 = createGameCard("Card 1")
        val card2 = createGameCard("Card 2")
        val card3 = createGameCard("Card 3")
        val marketCards = listOf(card1, card2, card3)
        
        val combination1 = Combination(DieValues(listOf(DieValue(4, 2))), 0)
        val combination2 = Combination(DieValues(listOf(DieValue(6, 4))), 0)
        val combination3 = Combination(DieValues(listOf(DieValue(8, 6))), 0)
        val combinations = createCombinations(listOf(combination1, combination2, combination3))
        
        // Each combination can buy different cards
        every { mockPlayer.cardsInHand } returns emptyList()
        every { mockEvaluateCardPurchases(marketCards, any(), combination1) } returns listOf(card1)
        every { mockEvaluateCardPurchases(marketCards, any(), combination2) } returns listOf(card2)
        every { mockEvaluateCardPurchases(marketCards, any(), combination3) } returns listOf(card3)
        
        // Director prefers card2 over all others
        every { mockDecisionDirector.bestCardPurchase(listOf(card1)) } returns card1
        every { mockDecisionDirector.bestCardPurchase(listOf(card2)) } returns card2
        every { mockDecisionDirector.bestCardPurchase(listOf(card3)) } returns card3
        every { mockDecisionDirector.bestCardPurchase(listOf(card1, card2, card3)) } returns card2

        // Act
        val result = SUT(mockPlayer, combinations, marketCards)
        
        // Assert
        assertEquals(card2, result?.card, "Should return card2 as chosen by the decision director")
        assertEquals(combination2, result?.combination, "Should return the combination that was used for card2")
        
        // Verify that the final decision was made with all best cards
        verify { mockDecisionDirector.bestCardPurchase(listOf(card1, card2, card3)) }
    }
    
    private fun createCombinations(combinations: List<Combination>): Combinations {
        return Combinations(combinations)
    }
    
    private fun createGameCard(name: String): GameCard {
        return GameCard(
            id = 1,
            name = name,
            type = FlourishType.ROOT,
            resilience = 1,
            cost = Cost(emptyList()),
            primaryEffect = CardEffect.ADD_TO_TOTAL,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )
    }
} 
