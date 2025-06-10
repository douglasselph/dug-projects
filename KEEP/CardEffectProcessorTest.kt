package dugsolutions.leaf.player.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.random.die.Die
import io.mockk.coEvery
import io.mockk.coVerify
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

class aCardEffectProcessorTest {
    companion object {
        private const val CARD_ID_1 = 1
        private const val TEST_VALUE = 3
        private const val DRAW_CARD_COUNT = 2
        private const val DRAW_DIE_COUNT = 1
        private const val DISCARD_COUNT = 1
    }

    private val mockCanProcessMatchEffect: CanProcessMatchEffect = mockk(relaxed = true)
    private val mockShouldProcessMatchEffect: ShouldProcessMatchEffect = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockAppliedEffectUseCase: AppliedEffectUseCase = mockk(relaxed = true)
    private val mockFlowerCardMatchValue: FlowerCardMatchValue = mockk(relaxed = true)
    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockCard: GameCard = mockk(relaxed = true) {
        every { id } returns CARD_ID_1
    }
    private val SUT: CardEffectProcessor = CardEffectProcessor(
        mockCanProcessMatchEffect, mockShouldProcessMatchEffect, mockAppliedEffectUseCase, mockFlowerCardMatchValue, mockChronicle
    )

    @BeforeEach
    fun setup() {
        every { mockPlayer.removeCardFromHand(any()) } returns true
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(false)
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH
        every { mockShouldProcessMatchEffect(any()) } returns true
        every { mockAppliedEffectUseCase(CardEffect.DRAW_CARD, DRAW_CARD_COUNT) } returns AppliedEffect.DrawCards(DRAW_CARD_COUNT)
        every { mockAppliedEffectUseCase(CardEffect.DRAW_CARD_COMPOST, DRAW_CARD_COUNT) } returns AppliedEffect.DrawCards(DRAW_CARD_COUNT, fromCompost = true)
        every { mockAppliedEffectUseCase(CardEffect.DRAW_DIE, DRAW_DIE_COUNT) } returns AppliedEffect.DrawDice(DRAW_DIE_COUNT)
        every { mockAppliedEffectUseCase(CardEffect.DISCARD, DISCARD_COUNT) } returns AppliedEffect.Discard(DISCARD_COUNT)
        every { mockAppliedEffectUseCase(CardEffect.UPGRADE_ANY, TEST_VALUE) } returns AppliedEffect.UpgradeDie()

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
        every { mockCard.primaryValue } returns DRAW_CARD_COUNT
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns DRAW_DIE_COUNT
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

    @Test
    fun processCardEffect_withDrawCardsEffect_returnsCorrectCountAndFromCompost() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD_COMPOST
        every { mockCard.primaryValue } returns DRAW_CARD_COUNT
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns null

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        val drawCards = result[0] as AppliedEffect.DrawCards
        assertEquals(DRAW_CARD_COUNT, drawCards.count)
        assertTrue(drawCards.fromCompost)
    }

    @Test
    fun processCardEffect_withTrashEffect_setsTrashAfterUseCorrectly() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.UPGRADE_ANY
        every { mockCard.primaryValue } returns TEST_VALUE
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns CardEffect.DRAW_CARD
        every { mockCard.trashValue } returns DRAW_CARD_COUNT
        coEvery { mockPlayer.decisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is AppliedEffect.UpgradeDie)
        assertTrue(result[1] is AppliedEffect.DrawCards)
        val drawCards = result[1] as AppliedEffect.DrawCards
        assertEquals(DRAW_CARD_COUNT, drawCards.count)

        // Verify card IS removed from hand for trash effects
        verify(exactly = 1) { mockPlayer.removeCardFromHand(CARD_ID_1) }
    }

    @Test
    fun processCardEffect_whenMatchEffectWithDieCost_discardsMatchingDie() = runBlocking {
        // Arrange
        val mockDie = mockk<Die>(relaxed = true)
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

    @Test
    fun processCardEffect_whenMatchEffectWithFlowerBonus_addsFlowerBonusToMatchValue() = runBlocking {
        // Arrange
        val flowerBonus = 3
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns DRAW_CARD_COUNT
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns DRAW_DIE_COUNT
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(true)
        coEvery { mockFlowerCardMatchValue(mockPlayer, mockCard) } returns flowerBonus
        every { mockAppliedEffectUseCase(CardEffect.DRAW_DIE, any()) } returns AppliedEffect.DrawDice(DRAW_DIE_COUNT + flowerBonus)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        assertTrue(result[1] is AppliedEffect.DrawDice)
        coVerify(exactly = 1) { mockFlowerCardMatchValue(mockPlayer, mockCard) }
        verify(exactly = 1) { mockAppliedEffectUseCase(CardEffect.DRAW_DIE, DRAW_DIE_COUNT + flowerBonus) }
    }

    @Test
    fun processCardEffect_whenMatchEffectWithZeroFlowerBonus_usesOriginalMatchValue() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns DRAW_CARD_COUNT
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns DRAW_DIE_COUNT
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(true)
        coEvery { mockFlowerCardMatchValue(mockPlayer, mockCard) } returns 0

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        assertTrue(result[1] is AppliedEffect.DrawDice)
        coVerify(exactly = 1) { mockFlowerCardMatchValue(mockPlayer, mockCard) }
        verify(exactly = 1) { mockAppliedEffectUseCase(CardEffect.DRAW_DIE, DRAW_DIE_COUNT) }
    }

    @Test
    fun processCardEffect_whenMatchEffectNotProcessed_doesNotCallFlowerCardMatchValue() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns CardEffect.DRAW_CARD
        every { mockCard.primaryValue } returns DRAW_CARD_COUNT
        every { mockCard.matchEffect } returns CardEffect.DRAW_DIE
        every { mockCard.matchValue } returns DRAW_DIE_COUNT
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(false)

        // Act
        val result = SUT(mockCard, mockPlayer)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result[0] is AppliedEffect.DrawCards)
        coVerify(exactly = 0) { mockFlowerCardMatchValue(mockPlayer, mockCard) }
    }
} 
