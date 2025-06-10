package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.player.decisions.local.EffectBattleScore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectUseOpponentCardTest {

    companion object {
        private const val ROOT_CARD_1_ID = 1
        private const val ROOT_CARD_2_ID = 2
        private const val CANOPY_CARD_ID = 3
        private const val FLOWER_CARD_ID = 4
        private const val USE_OPPONENT_CARD_ID = 5
        private const val LOW_SCORE = 1
        private const val HIGH_SCORE = 5
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val fakeTarget = PlayerTD.create2(2)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockEffectBattleScore: EffectBattleScore = mockk(relaxed = true)
    private val mockRootCard1: GameCard = mockk {
        every { id } returns ROOT_CARD_1_ID
        every { type } returns FlourishType.ROOT
        every { primaryEffect } returns CardEffect.DRAW_CARD
        every { primaryValue } returns LOW_SCORE
    }
    private val mockRootCard2: GameCard = mockk {
        every { id } returns ROOT_CARD_2_ID
        every { type } returns FlourishType.ROOT
        every { primaryEffect } returns CardEffect.DRAW_CARD
        every { primaryValue } returns HIGH_SCORE
    }
    private val mockCanopyCard: GameCard = mockk {
        every { id } returns CANOPY_CARD_ID
        every { type } returns FlourishType.CANOPY
        every { primaryEffect } returns CardEffect.DRAW_CARD
        every { primaryValue } returns LOW_SCORE
    }
    private val mockFlowerCard: GameCard = mockk {
        every { id } returns FLOWER_CARD_ID
        every { type } returns FlourishType.FLOWER
        every { primaryEffect } returns CardEffect.DRAW_CARD
        every { primaryValue } returns HIGH_SCORE
    }
    private val mockUseOpponentCard: GameCard = mockk {
        every { id } returns USE_OPPONENT_CARD_ID
        every { type } returns FlourishType.ROOT
        every { primaryEffect } returns CardEffect.USE_OPPONENT_CARD
        every { primaryValue } returns HIGH_SCORE
    }

    private val SUT = EffectUseOpponentCard(mockEffectBattleScore, mockChronicle)

    @BeforeEach
    fun setup() {
        fakePlayer.useDeckManager = false
        fakeTarget.useDeckManager = false
        fakePlayer.cardsToPlay.clear()
        every { mockEffectBattleScore(CardEffect.DRAW_CARD, LOW_SCORE) } returns LOW_SCORE
        every { mockEffectBattleScore(CardEffect.DRAW_CARD, HIGH_SCORE) } returns HIGH_SCORE
    }

    @Test
    fun invoke_whenTargetHandEmpty_doesNothing() {
        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertTrue(fakePlayer.cardsToPlay.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenOnlyFlowerCards_doesNothing() {
        // Arrange
        fakeTarget.addCardToHand(mockFlowerCard)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertTrue(fakePlayer.cardsToPlay.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenOnlyUseOpponentCards_doesNothing() {
        // Arrange
        fakeTarget.addCardToHand(mockUseOpponentCard)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertTrue(fakePlayer.cardsToPlay.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_withSingleValidCard_addsToCardsToPlay() {
        // Arrange
        fakeTarget.addCardToHand(mockRootCard1)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(1, fakePlayer.cardsToPlay.size)
        assertEquals(mockRootCard1, fakePlayer.cardsToPlay[0])
        verify { mockChronicle(Moment.USE_OPPONENT_CARD(fakePlayer, mockRootCard1)) }
    }

    @Test
    fun invoke_withMultipleValidCards_selectsHighestScore() {
        // Arrange
        fakeTarget.addCardToHand(mockRootCard1)
        fakeTarget.addCardToHand(mockRootCard2)
        fakeTarget.addCardToHand(mockCanopyCard)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(1, fakePlayer.cardsToPlay.size)
        assertEquals(mockRootCard2, fakePlayer.cardsToPlay[0])
        verify { mockChronicle(Moment.USE_OPPONENT_CARD(fakePlayer, mockRootCard2)) }
    }

    @Test
    fun invoke_withMixedCardTypes_selectsHighestScoreValidCard() {
        // Arrange
        fakeTarget.addCardToHand(mockFlowerCard)
        fakeTarget.addCardToHand(mockUseOpponentCard)
        fakeTarget.addCardToHand(mockRootCard1)
        fakeTarget.addCardToHand(mockRootCard2)

        // Act
        SUT(fakePlayer, fakeTarget)

        // Assert
        assertEquals(1, fakePlayer.cardsToPlay.size)
        assertEquals(mockRootCard2, fakePlayer.cardsToPlay[0])
        verify { mockChronicle(Moment.USE_OPPONENT_CARD(fakePlayer, mockRootCard2)) }
    }
} 
