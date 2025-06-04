package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.domain.AppliedEffect
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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

    private lateinit var mockCanProcessMatchEffect: CanProcessMatchEffect
    private lateinit var mockShouldProcessMatchEffect: ShouldProcessMatchEffect
    private lateinit var mockChronicle: GameChronicle
    private lateinit var mockPlayer: Player
    private lateinit var mockCard: GameCard

    private lateinit var SUT: CardEffectProcessor

    @BeforeEach
    fun setup() {
        mockCanProcessMatchEffect = mockk(relaxed = true)
        mockShouldProcessMatchEffect = mockk(relaxed = true)
        mockChronicle = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockCard = mockk(relaxed = true) {
            every { id } returns CARD_ID_1
        }
        SUT = CardEffectProcessor(mockCanProcessMatchEffect, mockShouldProcessMatchEffect, mockChronicle)

        every { mockPlayer.removeCardFromHand(any()) } returns true
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(false)
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH
        every { mockShouldProcessMatchEffect(mockCard) } returns true
    }

    @Test
    fun processCardEffect_whenNoEffects_returnsEmptyList() = runBlocking {
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
    fun processCardEffect_whenOnlyPrimaryEffect_returnsPrimaryEffect() = runBlocking {
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
    fun processCardEffect_whenMatchEffectAndShouldProcess_returnsBothEffects() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(true)

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
    fun processCardEffect_whenMatchEffectAndShouldNotProcess_returnsOnlyPrimaryEffect() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(false)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)

        // Verify card is not removed from hand
        confirmVerified(mockPlayer)
    }

    @Test
    fun processCardEffect_whenMatchEffectAndShouldNotProcess_returnsBothEffects() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(true)
        every { mockShouldProcessMatchEffect(mockCard) } returns false

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)

        // Verify card is not removed from hand
        confirmVerified(mockPlayer)
    }

    @Test
    fun processCardEffect_whenTrashEffect_returnsAllEffectsAndRemovesCardFromHand() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns CardEffect.DISCARD
        every { mockCard.trashValue } returns 1
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH

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
    fun processCardEffect_whenAllEffects_returnsAllEffectsInOrderAndRemovesCard() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns CardEffect.DISCARD
        every { mockCard.trashValue } returns 1
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(true)
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH

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
    fun processEffect_forAllEffectTypes_returnsCorrectAppliedEffect(effectType: CardEffect) = runBlocking {
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
            else -> { /* Just skip any effect types not explicitly handled */
            }
        }
    }

    @Test
    fun processCardEffect_withDrawCardsEffect_returnsCorrectCountAndFromCompost() = runBlocking {
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
    fun processCardEffect_withMarketBenefitEffect_returnsCorrectTypeAndValues() = runBlocking {
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
    fun processCardEffect_withTrashEffect_setsTrashAfterUseCorrectly() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.UPGRADE_ANY
        every { mockCard.primaryValue } returns TEST_VALUE
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns CardEffect.DRAW_CARD
        every { mockCard.trashValue } returns TEST_VALUE
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH

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

    @Test
    fun processCardEffect_whenMatchEffectWithDieCost_discardsMatchingDie() = runBlocking {
        // Arrange
        val mockDie = mockk<dugsolutions.leaf.components.die.Die>(relaxed = true)
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns 2
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns 1
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(true, dieCost = mockDie)
        every { mockPlayer.discard(mockDie) } returns true

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        assertTrue(result[1] is AppliedEffect.DrawDice)
        verify(exactly = 1) { mockPlayer.discard(mockDie) }
    }
} 
