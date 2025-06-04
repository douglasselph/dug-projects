package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.components.die.DieValues
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.effect.EffectsList
import dugsolutions.leaf.player.domain.AppliedEffect
import io.mockk.mockk
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluateCardPurchasesTest {

    private val mockEffectsList = mockk<EffectsList>(relaxed = true)

    private val SUT: EvaluateCardPurchases = EvaluateCardPurchases()

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

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
        val result1 = SUT(marketCards, hasFlourishTypes, combination, mockEffectsList)
        val result2 = SUT(marketCards, hasFlourishTypes, combinationMissingDieMin, mockEffectsList)

        // Assert
        assertEquals(1, result1.size, "Card should be purchasable when all criteria are met")
        assertEquals(0, result2.size, "Card should not be purchasable when any criterion is not met")
    }

    @Test
    fun invoke_withMarketBenefitMatchingType_appliesCostReduction() {
        // Arrange
        val rootCard = createGameCard(
            "Root Card",
            Cost(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val marketCards = listOf(rootCard)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3))) // Total of 3
        val combination = Combination(dieValues, 0)
        
        val marketBenefit = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            costReduction = 3
        )
        every { mockEffectsList.iterator() } returns listOf(marketBenefit).iterator()

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

        // Assert
        assertEquals(1, result.size, "Card should be purchasable with cost reduction")
        assertEquals(rootCard, result[0])
    }

    @Test
    fun invoke_withMarketBenefitNonMatchingType_doesNotApplyCostReduction() {
        // Arrange
        val rootCard = createGameCard(
            "Root Card",
            Cost(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val marketCards = listOf(rootCard)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3))) // Total of 3
        val combination = Combination(dieValues, 0)
        
        val marketBenefit = AppliedEffect.MarketBenefit(
            type = FlourishType.CANOPY, // Different type
            costReduction = 3
        )
        every { mockEffectsList.iterator() } returns listOf(marketBenefit).iterator()

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

        // Assert
        assertEquals(0, result.size, "Card should not be purchasable without matching cost reduction")
    }

    @Test
    fun invoke_withMultipleMarketBenefits_appliesAllMatchingReductions() {
        // Arrange
        val rootCard = createGameCard(
            "Root Card",
            Cost(listOf(CostElement.TotalDiceMinimum(8)))
        )
        val marketCards = listOf(rootCard)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3))) // Total of 3
        val combination = Combination(dieValues, 0)
        
        val marketBenefit1 = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            costReduction = 3
        )
        val marketBenefit2 = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            costReduction = 2
        )
        val marketBenefit3 = AppliedEffect.MarketBenefit(
            type = FlourishType.CANOPY, // Non-matching type
            costReduction = 5
        )
        val allEffects = listOf(marketBenefit1, marketBenefit2, marketBenefit3)
        every { mockEffectsList.iterator() } returns allEffects.iterator()

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

        // Assert
        assertEquals(1, result.size, "Card should be purchasable with combined cost reductions (3+2=5, making cost 8-5=3)")
        assertEquals(rootCard, result[0])
    }

    @Test
    fun invoke_withMarketBenefitReducingCostToZero_makeCardPurchasable() {
        // Arrange
        val rootCard = createGameCard(
            "Root Card",
            Cost(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val marketCards = listOf(rootCard)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 1))) // Total of 1
        val combination = Combination(dieValues, 0)
        
        val marketBenefit = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            costReduction = 6 // More than enough reduction
        )
        every { mockEffectsList.iterator() } returns listOf(marketBenefit).iterator()

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

        // Assert
        assertEquals(1, result.size, "Card should be purchasable when cost reduction exceeds required amount")
        assertEquals(rootCard, result[0])
    }

    @Test
    fun invoke_withNoMarketBenefits_usesOriginalCost() {
        // Arrange
        val rootCard = createGameCard(
            "Root Card",
            Cost(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val marketCards = listOf(rootCard)
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3))) // Total of 3
        val combination = Combination(dieValues, 0)
        
        every { mockEffectsList.iterator() } returns emptyList<AppliedEffect>().iterator()

        // Act
        val result = SUT(marketCards, playerHasFlourishTypes, combination, mockEffectsList)

        // Assert
        assertEquals(0, result.size, "Card should not be purchasable without sufficient dice total")
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
            trashValue = 0,
            thorn = 0
        )
    }
} 
