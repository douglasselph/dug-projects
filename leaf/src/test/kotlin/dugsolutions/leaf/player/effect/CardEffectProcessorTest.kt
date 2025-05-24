package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.domain.AppliedEffect
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals

class CardEffectProcessorTest {
    companion object {
        private const val CARD_ID_1 = 1
        private const val TEST_VALUE = 3
    }

    private lateinit var mockShouldProcessMatchEffect: ShouldProcessMatchEffect
    private lateinit var mockPlayer: Player
    private lateinit var mockCard: GameCard

    private lateinit var SUT: CardEffectProcessor

    @BeforeEach
    fun setup() {
        mockShouldProcessMatchEffect = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockCard = mockk(relaxed = true) {
            every { id } returns CARD_ID_1
        }
        SUT = CardEffectProcessor(mockShouldProcessMatchEffect)

        every { mockPlayer.removeCardFromHand(any()) } returns true
        every { mockShouldProcessMatchEffect(mockCard, mockPlayer) } returns false
        every { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH
    }

    @Test
    fun processCardEffect_whenNoEffects_returnsEmptyList() {
        // Arrange
        every { mockCard.primaryEffect } returns null
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns null

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(emptyList(), result)
        
        // Verify card is not removed from hand
        confirmVerified(mockPlayer)
    }

    @Test
    fun processCardEffect_whenOnlyPrimaryEffect_returnsPrimaryEffect() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns null

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        assertEquals(2, (result[0] as AppliedEffect.DrawCards).count)
        
        // Verify card is not removed from hand
        confirmVerified(mockPlayer)
    }

    @Test
    fun processCardEffect_whenMatchEffectAndShouldProcess_returnsBothEffects() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns null
        every { mockShouldProcessMatchEffect(mockCard, mockPlayer) } returns true

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        assertTrue(result[1] is AppliedEffect.DrawDice)
        
        // Verify card is not removed from hand
        confirmVerified(mockPlayer)
    }

    @Test
    fun processCardEffect_whenMatchEffectAndShouldNotProcess_returnsOnlyPrimaryEffect() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns null
        every { mockShouldProcessMatchEffect(mockCard, mockPlayer) } returns false

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        
        // Verify card is not removed from hand
        confirmVerified(mockPlayer)
    }

    @Test
    fun processCardEffect_whenTrashEffect_returnsAllEffectsAndRemovesCardFromHand() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns CardEffect.DISCARD
        every { mockCard.trashValue } returns 1
        every { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        assertTrue(result[1] is AppliedEffect.Discard)

        // Verify card IS removed from hand for trash effects
        verify(exactly = 1) { mockPlayer.removeCardFromHand(CARD_ID_1) }
    }

    @Test
    fun processCardEffect_whenAllEffects_returnsAllEffectsInOrderAndRemovesCard() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns CardEffect.DISCARD
        every { mockCard.trashValue } returns 1
        every { mockShouldProcessMatchEffect(mockCard, mockPlayer) } returns true
        every { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(3, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        assertTrue(result[1] is AppliedEffect.DrawDice)
        assertTrue(result[2] is AppliedEffect.Discard)

        // Verify card IS removed from hand for trash effects
        verify(exactly = 1) { mockPlayer.removeCardFromHand(CARD_ID_1) }
    }
    
    @ParameterizedTest
    @EnumSource(CardEffect::class)
    fun processEffect_forAllEffectTypes_returnsCorrectAppliedEffect(effectType: CardEffect) {
        // Arrange
        every { mockCard.primaryEffect } returns effectType
        every { mockCard.primaryValue } returns TEST_VALUE
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns null

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        val appliedEffect = result[0]
        
        // Verify the applied effect type matches the expected type for the card effect
        when (effectType) {
            CardEffect.ADD_TO_DIE -> assertTrue(appliedEffect is AppliedEffect.AdjustDieRoll)
            CardEffect.ADD_TO_TOTAL -> assertTrue(appliedEffect is AppliedEffect.AddToTotal)
            CardEffect.ADJUST_BY -> assertTrue(appliedEffect is AppliedEffect.AdjustDieRoll)
            CardEffect.ADJUST_TO_MAX -> assertTrue(appliedEffect is AppliedEffect.AdjustDieToMax)
            CardEffect.DISCARD -> assertTrue(appliedEffect is AppliedEffect.Discard)
            CardEffect.DISCARD_CARD -> assertTrue(appliedEffect is AppliedEffect.Discard)
            CardEffect.DISCARD_DIE -> assertTrue(appliedEffect is AppliedEffect.Discard)
            CardEffect.DRAW_CARD -> assertTrue(appliedEffect is AppliedEffect.DrawCards)
            CardEffect.DRAW_CARD_COMPOST -> assertTrue(appliedEffect is AppliedEffect.DrawCards)
            CardEffect.DRAW_DIE -> assertTrue(appliedEffect is AppliedEffect.DrawDice)
            CardEffect.DRAW_DIE_ANY -> assertTrue(appliedEffect is AppliedEffect.DrawDice)
            CardEffect.DRAW_DIE_COMPOST -> assertTrue(appliedEffect is AppliedEffect.DrawDice)
            CardEffect.DRAW_THEN_DISCARD -> assertTrue(appliedEffect is AppliedEffect.DrawThenDiscard)
            CardEffect.GAIN_FREE_ROOT -> assertTrue(appliedEffect is AppliedEffect.MarketBenefit)
            CardEffect.GAIN_FREE_CANOPY -> assertTrue(appliedEffect is AppliedEffect.MarketBenefit)
            CardEffect.GAIN_FREE_VINE -> assertTrue(appliedEffect is AppliedEffect.MarketBenefit)
            CardEffect.REDUCE_COST_ROOT -> assertTrue(appliedEffect is AppliedEffect.MarketBenefit)
            CardEffect.REDUCE_COST_CANOPY -> assertTrue(appliedEffect is AppliedEffect.MarketBenefit)
            CardEffect.REDUCE_COST_VINE -> assertTrue(appliedEffect is AppliedEffect.MarketBenefit)
            CardEffect.REROLL_ACCEPT_2ND -> assertTrue(appliedEffect is AppliedEffect.RerollDie)
            CardEffect.REROLL_ALL_MAX -> assertTrue(appliedEffect is AppliedEffect.RerollDie)
            CardEffect.REROLL_TAKE_BETTER -> assertTrue(appliedEffect is AppliedEffect.RerollDie)
            CardEffect.RETAIN_CARD -> assertTrue(appliedEffect is AppliedEffect.RetainCard)
            CardEffect.RETAIN_DIE -> assertTrue(appliedEffect is AppliedEffect.RetainDie)
            CardEffect.RETAIN_DIE_REROLL -> assertTrue(appliedEffect is AppliedEffect.RetainDie)
            CardEffect.UPGRADE_ANY_RETAIN -> assertTrue(appliedEffect is AppliedEffect.UpgradeDie)
            CardEffect.UPGRADE_ANY -> assertTrue(appliedEffect is AppliedEffect.UpgradeDie)
            CardEffect.UPGRADE_D4_D6 -> assertTrue(appliedEffect is AppliedEffect.UpgradeDie)
            CardEffect.DEFLECT -> assertTrue(appliedEffect is AppliedEffect.DeflectDamage)
            CardEffect.REUSE_CARD -> assertTrue(appliedEffect is AppliedEffect.Reuse)
            CardEffect.REPLAY_VINE -> assertTrue(appliedEffect is AppliedEffect.Replay)
            else -> { /* Just skip any effect types not explicitly handled */ }
        }
    }
    
    @Test
    fun processCardEffect_withDrawCardsEffect_returnsCorrectCountAndFromCompost() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD_COMPOST
        every { mockCard.primaryValue } returns TEST_VALUE
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns null

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        val drawCards = result[0] as AppliedEffect.DrawCards
        assertEquals(TEST_VALUE, drawCards.count)
        assertTrue(drawCards.fromCompost)
    }
    
    @Test
    fun processCardEffect_withMarketBenefitEffect_returnsCorrectTypeAndValues() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.GAIN_FREE_CANOPY
        every { mockCard.primaryValue } returns TEST_VALUE
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns null

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.MarketBenefit)
        val marketBenefit = result[0] as AppliedEffect.MarketBenefit
        assertEquals(FlourishType.CANOPY, marketBenefit.type)
        assertEquals(0, marketBenefit.costReduction)
        assertTrue(marketBenefit.isFree)
    }
    
    @Test
    fun processCardEffect_withTrashEffect_setsTrashAfterUseCorrectly() {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.UPGRADE_ANY
        every { mockCard.primaryValue } returns TEST_VALUE
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns CardEffect.DRAW_CARD
        every { mockCard.trashValue } returns TEST_VALUE
        every { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is AppliedEffect.UpgradeDie)
        assertTrue(result[1] is AppliedEffect.DrawCards)
        val drawCards = result[1] as AppliedEffect.DrawCards
        assertEquals(TEST_VALUE, drawCards.count)

        // Verify card IS removed from hand for trash effects
        verify(exactly = 1) { mockPlayer.removeCardFromHand(CARD_ID_1) }
    }
} 
