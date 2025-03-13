package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.di.GameCardsFactory
import dugsolutions.leaf.game.purchase.evaluator.PurchaseCardEvaluator
import dugsolutions.leaf.game.purchase.evaluator.PurchaseDieEvaluator
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.tool.RandomizerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DecisionDirectorTest {

    private lateinit var defaultAcquireDecision: DecisionAcquireSelectCoreStrategy
    private lateinit var defaultDamageAbsorptionDecision: DecisionDamageAbsorptionCoreStrategy
    private lateinit var defaultDrawCountDecisionDefault: DecisionDrawCountCoreStrategy
    private lateinit var defaultShouldProcessTrashEffect: DecisionShouldProcessTrashEffectCoreStrategy
    private lateinit var defaultDecisionBestPurchase: DecisionBestCardPurchaseCoreStrategy
    private lateinit var mockPlayer: Player
    private lateinit var mockGameCard: GameCard
    private lateinit var mockBestCard: PurchaseCardEvaluator.BestChoice
    private lateinit var mockBestDie: PurchaseDieEvaluator.BestChoice
    private lateinit var cardManager: CardManager

    private lateinit var SUT: DecisionDirector

    @BeforeEach
    fun setup() {
        val randomizer = RandomizerTD()
        val costScore: CostScore = mockk(relaxed = true)
        val gameCardsFactory = GameCardsFactory(randomizer, costScore)
        cardManager = CardManager(gameCardsFactory)
        cardManager.loadCards(FakeCards.ALL_CARDS)
        defaultAcquireDecision = mockk(relaxed = true)
        defaultDamageAbsorptionDecision = mockk(relaxed = true)
        defaultDrawCountDecisionDefault = mockk(relaxed = true)
        defaultShouldProcessTrashEffect = mockk(relaxed = true)
        defaultDecisionBestPurchase = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockGameCard = mockk(relaxed = true)
        mockBestCard = mockk(relaxed = true)
        mockBestDie = mockk(relaxed = true)
        SUT = DecisionDirector(mockPlayer, cardManager)
    }

    @Test
    fun setAcquireDecision_updatesAcquireDecision() {
        // Arrange
        val newAcquireDecision = mockk<DecisionAcquireSelectCoreStrategy>(relaxed = true)

        // Act
        SUT.acquireDecision = newAcquireDecision

        // Assert
        assertEquals(newAcquireDecision, SUT.acquireDecision)
    }

    @Test
    fun setDamageAbsorptionDecision_updatesDamageAbsorptionDecision() {
        // Arrange
        val newDamageAbsorptionDecision = mockk<DecisionDamageAbsorptionCoreStrategy>(relaxed = true)

        // Act
        SUT.damageAbsorptionDecision = newDamageAbsorptionDecision

        // Assert
        assertEquals(newDamageAbsorptionDecision, SUT.damageAbsorptionDecision)
    }

    @Test
    fun setDrawCountDecision_updatesDrawCountDecision() {
        // Arrange
        val newDrawCountDecision = mockk<DecisionDrawCountCoreStrategy>(relaxed = true)

        // Act
        SUT.drawCountDecision = newDrawCountDecision

        // Assert
        assertEquals(newDrawCountDecision, SUT.drawCountDecision)
    }

    @Test
    fun setShouldProcessTrashEffect_updatesShouldProcessTrashEffect() {
        // Arrange
        val newShouldProcessTrashEffect = mockk<DecisionShouldProcessTrashEffectCoreStrategy>(relaxed = true)

        // Act
        SUT.shouldProcessTrashEffect = newShouldProcessTrashEffect

        // Assert
        assertEquals(newShouldProcessTrashEffect, SUT.shouldProcessTrashEffect)
    }

    @Test
    fun setBestCardPurchase_updatesBestCardPurchase() {
        // Arrange
        val newBestCardPurchase = mockk<DecisionBestCardPurchaseCoreStrategy>(relaxed = true)

        // Act
        SUT.bestCardPurchase = newBestCardPurchase

        // Assert
        assertEquals(newBestCardPurchase, SUT.bestCardPurchase)
    }

    @Test
    fun acquireDecision_delegatesToAcquireDecision() {
        // Arrange
        val expectedResult = mockk<DecisionAcquireSelect.BuyItem>(relaxed = true)
        every { defaultAcquireDecision(mockBestCard, mockBestDie) } returns expectedResult

        // Act
        val result = SUT.acquireDecision(mockBestCard, mockBestDie)

        // Assert
        assertEquals(expectedResult, result)
        verify { defaultAcquireDecision(mockBestCard, mockBestDie) }
    }

    @Test
    fun damageAbsorptionDecision_delegatesToDamageAbsorptionDecision() {
        // Arrange
        val expectedResult = mockk<DecisionDamageAbsorption.Result>(relaxed = true)
        every { defaultDamageAbsorptionDecision() } returns expectedResult

        // Act
        val result = SUT.damageAbsorptionDecision()

        // Assert
        assertEquals(expectedResult, result)
        verify { defaultDamageAbsorptionDecision() }
    }

    @Test
    fun drawCountDecision_delegatesToDrawCountDecision() {
        // Arrange
        val expectedResult = mockk<Int>(relaxed = true)
        every { defaultDrawCountDecisionDefault() } returns expectedResult

        // Act
        val result = SUT.drawCountDecision()

        // Assert
        assertEquals(expectedResult, result)
        verify { defaultDrawCountDecisionDefault() }
    }

    @Test
    fun shouldProcessTrashEffect_delegatesToShouldProcessTrashEffect() {
        // Arrange
        val expectedResult = true
        every { defaultShouldProcessTrashEffect(mockGameCard) } returns expectedResult

        // Act
        val result = SUT.shouldProcessTrashEffect(mockGameCard)

        // Assert
        assertEquals(expectedResult, result)
        verify { defaultShouldProcessTrashEffect(mockGameCard) }
    }

    @Test
    fun bestCardPurchase_delegatesToBestCardPurchase() {
        // Arrange
        val marketCards = listOf(mockGameCard)
        every { defaultDecisionBestPurchase(marketCards) } returns mockGameCard

        // Act
        val result = SUT.bestCardPurchase(marketCards)

        // Assert
        assertEquals(mockGameCard, result)
        verify { defaultDecisionBestPurchase(marketCards) }
    }
} 
