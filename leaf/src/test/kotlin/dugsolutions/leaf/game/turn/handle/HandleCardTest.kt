package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.decisions.local.ShouldAskTrashEffect
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.effect.CanProcessMatchEffect
import dugsolutions.leaf.player.effect.FlowerCardMatchValue
import dugsolutions.leaf.player.effect.ShouldProcessMatchEffect
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleCardTest {

    companion object {
        private const val CARD_ID = 1
        private const val PRIMARY_VALUE = 5
        private const val MATCH_VALUE = 3
        private const val TRASH_VALUE = 7
        private const val FLOWER_MATCH_VALUE = 2
        private val primaryEffect = CardEffect.REUSE_CARD
        private val matchEffect = CardEffect.DRAW_CARD
        private val trashEffect = CardEffect.DRAW_DIE
    }

    // Subject under test

    // Dependencies
    private val mockHandleCardEffect: HandleCardEffect = mockk(relaxed = true)
    private val mockCanProcessMatchEffect: CanProcessMatchEffect = mockk(relaxed = true)
    private val mockShouldProcessMatchEffect: ShouldProcessMatchEffect = mockk(relaxed = true)
    private val mockFlowerCardMatchValue: FlowerCardMatchValue = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockDecisionDirector = mockk<DecisionDirector>(relaxed = true)
    private val mockShouldAskTrashEffect = mockk<ShouldAskTrashEffect>(relaxed = true)

    private val delayedEffectList = mutableListOf<AppliedEffect>()
    private val sampleDie = SampleDie()
    private val mockDie = sampleDie.d6

    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockTarget: Player = mockk(relaxed = true)
    private val mockCard: GameCard = mockk(relaxed = true)

    private val SUT: HandleCard = HandleCard(
        mockHandleCardEffect,
        mockCanProcessMatchEffect,
        mockShouldProcessMatchEffect,
        mockShouldAskTrashEffect,
        mockFlowerCardMatchValue,
        mockChronicle
    )

    @BeforeEach
    fun setup() {
        every { mockCard.id } returns CARD_ID
    }

    @Test
    fun invoke_whenPrimaryEffectExists_processesPrimaryEffect() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns primaryEffect
        every { mockCard.primaryValue } returns PRIMARY_VALUE
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns null

        // Act
        SUT(mockPlayer, mockTarget, mockCard)

        // Assert
        verify { mockHandleCardEffect(mockPlayer, mockTarget, primaryEffect, PRIMARY_VALUE) }
    }

    @Test
    fun invoke_whenMatchEffectPossibleAndShouldProcess_withDieCost_processesMatchEffect() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns null
        every { mockCard.matchEffect } returns matchEffect
        every { mockCard.matchValue } returns MATCH_VALUE
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(possible = true, dieCost = mockDie)
        every { mockShouldProcessMatchEffect(mockCard) } returns true
        coEvery { mockFlowerCardMatchValue(mockPlayer, mockCard) } returns FLOWER_MATCH_VALUE
        every { mockPlayer.discard(mockDie) } returns true

        // Act
        SUT(mockPlayer, mockTarget, mockCard)

        // Assert
        verify { mockChronicle(Moment.DISCARD_DIE(mockPlayer, mockDie)) }
        verify { mockPlayer.discard(mockDie) }
        verify { mockHandleCardEffect(mockPlayer, mockTarget, matchEffect, MATCH_VALUE + FLOWER_MATCH_VALUE) }
    }

    @Test
    fun invoke_whenMatchEffectPossibleAndShouldProcess_noDieCost_processesMatchEffect() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns null
        every { mockCard.matchEffect } returns matchEffect
        every { mockCard.matchValue } returns MATCH_VALUE
        every { mockCard.trashEffect } returns null
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(possible = true, dieCost = null)
        every { mockShouldProcessMatchEffect(mockCard) } returns true
        coEvery { mockFlowerCardMatchValue(mockPlayer, mockCard) } returns FLOWER_MATCH_VALUE

        // Act
        SUT(mockPlayer, mockTarget, mockCard)

        // Assert
        verify(exactly = 0) { mockPlayer.discard(any<Die>()) }
        verify { mockHandleCardEffect(mockPlayer, mockTarget, matchEffect, MATCH_VALUE + FLOWER_MATCH_VALUE) }
    }

    @Test
    fun invoke_whenMatchEffectNotPossible_doesNothing() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns null
        every { mockCard.matchEffect } returns matchEffect
        every { mockCanProcessMatchEffect(mockCard, mockPlayer) } returns CanProcessMatchEffect.Result(possible = false)
        every { mockCard.trashEffect } returns null

        // Act
        SUT(mockPlayer, mockTarget, mockCard)

        // Assert
        verify(exactly = 0) { mockHandleCardEffect(any(), any(), any(), any()) }
    }

    @Test
    fun invoke_whenTrashEffectAndDecisionIsTrash_processesTrashEffect() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns null
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns trashEffect
        every { mockCard.trashValue } returns TRASH_VALUE
        every { mockPlayer.decisionDirector } returns mockDecisionDirector
        coEvery { mockShouldAskTrashEffect(mockPlayer, mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH
        every { mockPlayer.removeCardFromHand(mockCard.id) } returns true

        // Act
        SUT(mockPlayer, mockTarget, mockCard)

        // Assert
        verify { mockHandleCardEffect(mockPlayer, mockTarget, trashEffect, TRASH_VALUE) }
        verify { mockPlayer.removeCardFromHand(CARD_ID) }
        verify { mockChronicle(Moment.TRASH_FOR_EFFECT(mockPlayer, mockCard, DecisionShouldProcessTrashEffect.Result.TRASH)) }
    }

    @Test
    fun invoke_whenTrashEffectAndDecisionIsTrashIfNeeded_addsDelayedEffect() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns null
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns trashEffect
        every { mockPlayer.decisionDirector } returns mockDecisionDirector
        coEvery { mockShouldAskTrashEffect(mockPlayer, mockCard) } returns DecisionShouldProcessTrashEffect.Result.TRASH_IF_NEEDED
        every { mockPlayer.delayedEffectList } returns delayedEffectList

        // Act
        SUT(mockPlayer, mockTarget, mockCard)

        // Assert
        assert(delayedEffectList.any { it is AppliedEffect.TrashIfNeeded && it.card == mockCard })
        verify { mockChronicle(Moment.TRASH_FOR_EFFECT(mockPlayer, mockCard, DecisionShouldProcessTrashEffect.Result.TRASH_IF_NEEDED)) }
    }

    @Test
    fun invoke_whenTrashEffectAndDecisionIsDoNotTrash_doesNothing() = runBlocking {
        // Arrange
        every { mockCard.primaryEffect } returns null
        every { mockCard.matchEffect } returns null
        every { mockCard.trashEffect } returns trashEffect
        every { mockPlayer.decisionDirector } returns mockDecisionDirector
        coEvery { mockDecisionDirector.shouldProcessTrashEffect(mockCard) } returns DecisionShouldProcessTrashEffect.Result.DO_NOT_TRASH

        // Act
        SUT(mockPlayer, mockTarget, mockCard)

        // Assert
        verify(exactly = 0) { mockHandleCardEffect(any(), any(), any(), any()) }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify(exactly = 0) { mockPlayer.delayedEffectList.add(any()) }
    }
}
