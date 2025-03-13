package dugsolutions.leaf.game.purchase.evaluator

import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.purchase.domain.Combination
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluateCardPurchasesTest {

    private lateinit var SUT: EvaluateCardPurchases

    @BeforeEach
    fun setup() {
        SUT = EvaluateCardPurchases()
    }

    @Test
    fun invoke_withEmptyMarketCards_returnsEmptyList() {
        // Arrange
        val marketCards = emptyList<GameCard>()
        val playerHasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(listOf(DieValue(6, 3)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertTrue(result.isEmpty(), "Empty market cards should return empty result")
    }

    @Test
    fun invoke_withFreeCard_returnsCard() {
        // Arrange
        val freeCard = createGameCard("Free Card", Cost(emptyList()))
        val marketCards = listOf(freeCard)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertEquals(1, result.size, "Free card should be returned")
        assertEquals(freeCard, result[0])
    }

    @Test
    fun invoke_withFlourishTypePresent_returnsMatchingCards() {
        // Arrange
        val rootRequiringCard = createGameCard(
            "Root Card",
            Cost(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT)))
        )
        val canopyRequiringCard = createGameCard(
            "Canopy Card",
            Cost(listOf(CostElement.FlourishTypePresent(FlourishType.CANOPY)))
        )

        val marketCards = listOf(rootRequiringCard, canopyRequiringCard)
        val playerHasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertEquals(1, result.size, "Only the root requiring card should be returned")
        assertEquals(rootRequiringCard, result[0])
    }

    @Test
    fun invoke_withSingleDieExact_returnsMatchingCards() {
        // Arrange
        val exactDie3Card = createGameCard(
            "Exact 3 Card",
            Cost(listOf(CostElement.SingleDieExact(3)))
        )
        val exactDie5Card = createGameCard(
            "Exact 5 Card",
            Cost(listOf(CostElement.SingleDieExact(5)))
        )

        val marketCards = listOf(exactDie3Card, exactDie5Card)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertEquals(1, result.size, "Only the exact 3 card should be returned")
        assertEquals(exactDie3Card, result[0])
    }

    @Test
    fun invoke_withSingleDieMinimum_returnsMatchingCards() {
        // Arrange
        val minDie3Card = createGameCard(
            "Min 3 Card",
            Cost(listOf(CostElement.SingleDieMinimum(3)))
        )
        val minDie5Card = createGameCard(
            "Min 5 Card",
            Cost(listOf(CostElement.SingleDieMinimum(5)))
        )

        val marketCards = listOf(minDie3Card, minDie5Card)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertEquals(1, result.size, "Only the min 3 card should be returned")
        assertEquals(minDie3Card, result[0])
    }

    @Test
    fun invoke_withTotalDiceExact_returnsMatchingCards() {
        // Arrange
        val exactTotal7Card = createGameCard(
            "Exact Total 7 Card",
            Cost(listOf(CostElement.TotalDiceExact(7)))
        )
        val exactTotal10Card = createGameCard(
            "Exact Total 10 Card",
            Cost(listOf(CostElement.TotalDiceExact(10)))
        )

        val marketCards = listOf(exactTotal7Card, exactTotal10Card)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertEquals(1, result.size, "Only the exact total 7 card should be returned")
        assertEquals(exactTotal7Card, result[0])
    }

    @Test
    fun invoke_withTotalDiceMinimum_returnsMatchingCards() {
        // Arrange
        val minTotal5Card = createGameCard(
            "Min Total 5 Card",
            Cost(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val minTotal10Card = createGameCard(
            "Min Total 10 Card",
            Cost(listOf(CostElement.TotalDiceMinimum(10)))
        )

        val marketCards = listOf(minTotal5Card, minTotal10Card)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertEquals(1, result.size, "Only the min total 5 card should be returned")
        assertEquals(minTotal5Card, result[0])
    }

    @Test
    fun invoke_withCombinationAddToTotal_includesAddToTotalInCalculations() {
        // Arrange
        val minTotal8Card = createGameCard(
            "Min Total 8 Card",
            Cost(listOf(CostElement.TotalDiceMinimum(8)))
        )

        val marketCards = listOf(minTotal8Card)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3)))
        val addToTotal = 5
        val combination = Combination(dieValues, addToTotal)

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination)

        // Assert
        assertEquals(1, result.size, "Card should be purchasable due to addToTotal value")
        assertEquals(minTotal8Card, result[0])
    }

    @Test
    fun invoke_withMultipleCriteria_onlyReturnsCardsThatMeetAllCriteria() {
        // Arrange
        val complexCard = createGameCard(
            "Complex Card",
            Cost(
                listOf(
                    CostElement.FlourishTypePresent(FlourishType.ROOT),
                    CostElement.SingleDieMinimum(4),
                    CostElement.TotalDiceMinimum(7)
                )
            )
        )

        val marketCards = listOf(complexCard)

        // Case 1: Has all criteria
        val hasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Case 2: Missing die minimum
        val missingDieMinimum = DieValues(listOf(DieValue(6, 3), DieValue(8, 2), DieValue(6, 3)))
        val combinationMissingDieMin = Combination(missingDieMinimum, 0)

        // Act
        val result1 = SUT(marketCards, hasFlourishTypes, combination)
        val result2 = SUT(marketCards, hasFlourishTypes, combinationMissingDieMin)

        // Assert
        assertEquals(1, result1.size, "Card should be purchasable when all criteria are met")
        assertEquals(0, result2.size, "Card should not be purchasable when any criterion is not met")
    }

    private fun createGameCard(name: String, cost: Cost): GameCard {
        return GameCard(
            id = 1,
            name = name,
            type = FlourishType.ROOT,
            resilience = 1,
            cost = cost,
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
