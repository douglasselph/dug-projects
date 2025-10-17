package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.cost.CostAlternative
import dugsolutions.leaf.cards.cost.CostElement
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.game.acquire.domain.Combination
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.DieValues
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanPurchaseCardTest {

    companion object {
        private const val FREE_CARD_NAME = "Free Card"
        private const val ROOT_CARD_NAME = "Root Card"
        private const val CANOPY_CARD_NAME = "Canopy Card"
        private const val EXACT_3_CARD_NAME = "Exact 3 Card"
        private const val EXACT_5_CARD_NAME = "Exact 5 Card"
        private const val MIN_3_CARD_NAME = "Min 3 Card"
        private const val MIN_5_CARD_NAME = "Min 5 Card"
        private const val EXACT_TOTAL_7_CARD_NAME = "Exact Total 7 Card"
        private const val EXACT_TOTAL_10_CARD_NAME = "Exact Total 10 Card"
        private const val MIN_TOTAL_5_CARD_NAME = "Min Total 5 Card"
        private const val MIN_TOTAL_8_CARD_NAME = "Min Total 8 Card"
        private const val MIN_TOTAL_10_CARD_NAME = "Min Total 10 Card"
        private const val COMPLEX_CARD_NAME = "Complex Card"
    }

    private val sampleEffectsList = mutableListOf<AppliedEffect>()

    private val SUT: CanPurchaseCard = CanPurchaseCard()

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_withFreeCard_returnsTrue() {
        // Arrange
        val freeCard = createGameCard(FREE_CARD_NAME, Cost(emptyList()))
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(freeCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Free card should be purchasable")
    }

    @Test
    fun invoke_withFlourishTypePresentAndPlayerHasType_returnsTrue() {
        // Arrange
        val rootRequiringCard = createGameCard(
            ROOT_CARD_NAME,
            Cost.from(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT)))
        )
        val playerHasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(rootRequiringCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card requiring ROOT type should be purchasable when player has ROOT type")
    }

    @Test
    fun invoke_withFlourishTypePresentAndPlayerLacksType_returnsFalse() {
        // Arrange
        val canopyRequiringCard = createGameCard(
            CANOPY_CARD_NAME,
            Cost.from(listOf(CostElement.FlourishTypePresent(FlourishType.CANOPY)))
        )
        val playerHasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(emptyList())
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(canopyRequiringCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring CANOPY type should not be purchasable when player has only ROOT type")
    }

    @Test
    fun invoke_withSingleDieExactAndMatchingDie_returnsTrue() {
        // Arrange
        val exactDie3Card = createGameCard(
            EXACT_3_CARD_NAME,
            Cost.from(listOf(CostElement.SingleDieExact(3)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(exactDie3Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card requiring exact die value 3 should be purchasable when player has die with value 3")
    }

    @Test
    fun invoke_withSingleDieExactAndNoMatchingDie_returnsFalse() {
        // Arrange
        val exactDie5Card = createGameCard(
            EXACT_5_CARD_NAME,
            Cost.from(listOf(CostElement.SingleDieExact(5)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(exactDie5Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring exact die value 5 should not be purchasable when no die has value 5")
    }

    @Test
    fun invoke_withSingleDieMinimumAndSufficientDie_returnsTrue() {
        // Arrange
        val minDie3Card = createGameCard(
            MIN_3_CARD_NAME,
            Cost.from(listOf(CostElement.SingleDieMinimum(3)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(minDie3Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card requiring minimum die value 3 should be purchasable when player has die with value 4")
    }

    @Test
    fun invoke_withSingleDieMinimumAndInsufficientDie_returnsFalse() {
        // Arrange
        val minDie5Card = createGameCard(
            MIN_5_CARD_NAME,
            Cost.from(listOf(CostElement.SingleDieMinimum(5)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(minDie5Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring minimum die value 5 should not be purchasable when highest die value is 4")
    }

    @Test
    fun invoke_withTotalDiceExactAndMatchingTotal_returnsTrue() {
        // Arrange
        val exactTotal7Card = createGameCard(
            EXACT_TOTAL_7_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceExact(7)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(exactTotal7Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card requiring exact total 7 should be purchasable when dice total is 7")
    }

    @Test
    fun invoke_withTotalDiceExactAndNonMatchingTotal_returnsFalse() {
        // Arrange
        val exactTotal10Card = createGameCard(
            EXACT_TOTAL_10_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceExact(10)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(exactTotal10Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring exact total 10 should not be purchasable when dice total is 7")
    }

    @Test
    fun invoke_withTotalDiceMinimumAndSufficientTotal_returnsTrue() {
        // Arrange
        val minTotal5Card = createGameCard(
            MIN_TOTAL_5_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(minTotal5Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card requiring minimum total 5 should be purchasable when dice total is 7")
    }

    @Test
    fun invoke_withTotalDiceMinimumAndInsufficientTotal_returnsFalse() {
        // Arrange
        val minTotal10Card = createGameCard(
            MIN_TOTAL_10_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(10)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(minTotal10Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring minimum total 10 should not be purchasable when dice total is 7")
    }

    @Test
    fun invoke_withAlternativesExpensiveAvailable_returnsTrue() {
        // Arrange
        val sampleCard = createGameCard(
            MIN_TOTAL_5_CARD_NAME,
            Cost(
                listOf(
                    CostAlternative(
                        listOf(
                            CostElement.TotalDiceMinimum(15)
                        )
                    ),
                    CostAlternative(
                        listOf(
                            CostElement.FlourishTypePresent(FlourishType.ROOT),
                            CostElement.TotalDiceMinimum(9)
                        )
                    )
                )
            )
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(
            listOf(
                DieValue(6, 3),
                DieValue(20, 13)
            )
        )
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(sampleCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card requiring minimum total 15 or ROOT 9 should have been purchasable")
    }

    @Test
    fun invoke_withAlternativesInexpensiveAvailable_returnsTrue() {
        // Arrange
        val sampleCard = createGameCard(
            MIN_TOTAL_5_CARD_NAME,
            Cost(
                listOf(
                    CostAlternative(
                        listOf(
                            CostElement.TotalDiceMinimum(15)
                        )
                    ),
                    CostAlternative(
                        listOf(
                            CostElement.FlourishTypePresent(FlourishType.ROOT),
                            CostElement.TotalDiceMinimum(9)
                        )
                    )
                )
            )
        )
        val playerHasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(
            listOf(
                DieValue(6, 3),
                DieValue(8, 7)
            )
        )
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(sampleCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card requiring minimum total 15 or ROOT 9 should be purchasable with flourishType")
    }

    @Test
    fun invoke_notSatisfiedEvenWithAlternatives_returnsFalse() {
        val sampleCard = createGameCard(
            MIN_TOTAL_5_CARD_NAME,
            Cost(
                listOf(
                    CostAlternative(
                        listOf(
                            CostElement.TotalDiceMinimum(15)
                        )
                    ),
                    CostAlternative(
                        listOf(
                            CostElement.FlourishTypePresent(FlourishType.ROOT),
                            CostElement.TotalDiceMinimum(9)
                        )
                    )
                )
            )
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(
            listOf(
                DieValue(6, 3),
                DieValue(20, 11)
            )
        )
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(sampleCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring minimum total 15 or ROOT 9 should not have been purchasable")
    }

    @Test
    fun invoke_withAlternativesInexpensiveUnavailableWrongFlourishType_returnsFalse() {
        // Arrange
        val sampleCard = createGameCard(
            MIN_TOTAL_5_CARD_NAME,
            Cost(
                listOf(
                    CostAlternative(
                        listOf(
                            CostElement.TotalDiceMinimum(15)
                        )
                    ),
                    CostAlternative(
                        listOf(
                            CostElement.FlourishTypePresent(FlourishType.ROOT),
                            CostElement.TotalDiceMinimum(9)
                        )
                    )
                )
            )
        )
        val playerHasFlourishTypes = listOf(FlourishType.CANOPY)
        val dieValues = DieValues(
            listOf(
                DieValue(6, 3),
                DieValue(10, 7)
            )
        )
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(sampleCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring minimum total 15 or ROOT 9 should be not have been purchasable with flourishType")
    }

    @Test
    fun invoke_withAlternativesInexpensiveUnavailableNotEnough_returnsFalse() {
        // Arrange
        val sampleCard = createGameCard(
            MIN_TOTAL_5_CARD_NAME,
            Cost(
                listOf(
                    CostAlternative(
                        listOf(
                            CostElement.TotalDiceMinimum(15)
                        )
                    ),
                    CostAlternative(
                        listOf(
                            CostElement.FlourishTypePresent(FlourishType.ROOT),
                            CostElement.TotalDiceMinimum(9)
                        )
                    )
                )
            )
        )
        val playerHasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(
            listOf(
                DieValue(6, 3),
                DieValue(6, 2)
            )
        )
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(sampleCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card requiring minimum total 15 or ROOT 9 should be not have been purchasable with flourishType")
    }

    @Test
    fun invoke_withCombinationAddToTotal_includesAddToTotalInCalculations() {
        // Arrange
        val minTotal8Card = createGameCard(
            MIN_TOTAL_8_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(8)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3)))
        val addToTotal = 5
        val combination = Combination(dieValues, addToTotal)

        // Act
        val result = SUT(minTotal8Card, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card should be purchasable due to addToTotal value (3 + 5 = 8)")
    }

    @Test
    fun invoke_withMultipleCriteriaAllMet_returnsTrue() {
        // Arrange
        val complexCard = createGameCard(
            COMPLEX_CARD_NAME,
            Cost.from(
                listOf(
                    CostElement.FlourishTypePresent(FlourishType.ROOT),
                    CostElement.SingleDieMinimum(4),
                    CostElement.TotalDiceMinimum(7)
                )
            )
        )
        val hasFlourishTypes = listOf(FlourishType.ROOT)
        val dieValues = DieValues(listOf(DieValue(6, 3), DieValue(8, 4)))
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(complexCard, hasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card should be purchasable when all criteria are met")
    }

    @Test
    fun invoke_withMultipleCriteriaNotAllMet_returnsFalse() {
        // Arrange
        val complexCard = createGameCard(
            COMPLEX_CARD_NAME,
            Cost.from(
                listOf(
                    CostElement.FlourishTypePresent(FlourishType.ROOT),
                    CostElement.SingleDieMinimum(4),
                    CostElement.TotalDiceMinimum(7)
                )
            )
        )
        val hasFlourishTypes = listOf(FlourishType.ROOT)
        val missingDieMinimum = DieValues(listOf(DieValue(6, 3), DieValue(8, 2), DieValue(6, 3)))
        val combinationMissingDieMin = Combination(missingDieMinimum, 0)

        // Act
        val result = SUT(complexCard, hasFlourishTypes, combinationMissingDieMin, sampleEffectsList)

        // Assert
        assertFalse(result, "Card should not be purchasable when any criterion is not met")
    }

    @Test
    fun invoke_withMarketBenefitMatchingType_appliesCostReduction() {
        // Arrange
        val rootCard = createGameCard(
            ROOT_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3))) // Total of 3
        val combination = Combination(dieValues, 0)

        val marketBenefit = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            costReduction = 3
        )
        sampleEffectsList.add(marketBenefit)

        // Act
        val result = SUT(rootCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card should be purchasable with cost reduction (5 - 3 = 2, dice total = 3)")
    }

    @Test
    fun invoke_withMarketBenefitNonMatchingType_doesNotApplyCostReduction() {
        // Arrange
        val rootCard = createGameCard(
            ROOT_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3))) // Total of 3
        val combination = Combination(dieValues, 0)

        val marketBenefit = AppliedEffect.MarketBenefit(
            type = FlourishType.CANOPY, // Different type
            costReduction = 3
        )
        sampleEffectsList.add(marketBenefit)

        // Act
        val result = SUT(rootCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card should not be purchasable without matching cost reduction")
    }

    @Test
    fun invoke_withMultipleMarketBenefits_appliesAllMatchingReductions() {
        // Arrange
        val rootCard = createGameCard(
            ROOT_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(8)))
        )
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
        sampleEffectsList.addAll(listOf(marketBenefit1, marketBenefit2, marketBenefit3))

        // Act
        val result = SUT(rootCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card should be purchasable with combined cost reductions (8 - (3+2) = 3, dice total = 3)")
    }

    @Test
    fun invoke_withMarketBenefitReducingCostToZero_makeCardPurchasable() {
        // Arrange
        val rootCard = createGameCard(
            ROOT_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 1))) // Total of 1
        val combination = Combination(dieValues, 0)

        val marketBenefit = AppliedEffect.MarketBenefit(
            type = FlourishType.ROOT,
            costReduction = 6 // More than enough reduction
        )
        sampleEffectsList.add(marketBenefit)

        // Act
        val result = SUT(rootCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertTrue(result, "Card should be purchasable when cost reduction exceeds required amount")
    }

    @Test
    fun invoke_withNoMarketBenefits_usesOriginalCost() {
        // Arrange
        val rootCard = createGameCard(
            ROOT_CARD_NAME,
            Cost.from(listOf(CostElement.TotalDiceMinimum(5)))
        )
        val playerHasFlourishTypes = emptyList<FlourishType>()
        val dieValues = DieValues(listOf(DieValue(6, 3))) // Total of 3
        val combination = Combination(dieValues, 0)

        // Act
        val result = SUT(rootCard, playerHasFlourishTypes, combination, sampleEffectsList)

        // Assert
        assertFalse(result, "Card should not be purchasable without sufficient dice total")
    }

    private fun createGameCard(name: String, cost: Cost): GameCard {
        return GameCard(
            id = 1,
            name = name,
            type = FlourishType.ROOT,
            resilience = 8,
            nutrient = 1,
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
