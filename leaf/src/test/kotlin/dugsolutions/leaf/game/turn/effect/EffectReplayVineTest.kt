package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.PlayerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectReplayVineTest {

    companion object {
        private const val VINE_CARD_1_ID = 1
        private const val VINE_CARD_2_ID = 2
        private const val ROOT_CARD_ID = 3
        private const val REPLAY_VINE_CARD_ID = 4
    }

    private val fakePlayer = PlayerTD.create2(1)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val mockVineCard1: GameCard = mockk {
        every { id } returns VINE_CARD_1_ID
        every { type } returns FlourishType.VINE
        every { primaryEffect } returns CardEffect.DRAW_CARD
        every { matchEffect } returns CardEffect.DRAW_CARD
    }
    private val mockVineCard2: GameCard = mockk {
        every { id } returns VINE_CARD_2_ID
        every { type } returns FlourishType.VINE
        every { primaryEffect } returns CardEffect.DRAW_CARD
        every { matchEffect } returns CardEffect.DRAW_CARD
    }
    private val mockRootCard: GameCard = mockk {
        every { id } returns ROOT_CARD_ID
        every { type } returns FlourishType.ROOT
        every { primaryEffect } returns CardEffect.DRAW_CARD
        every { matchEffect } returns CardEffect.DRAW_CARD
    }
    private val mockReplayVineCard: GameCard = mockk {
        every { id } returns REPLAY_VINE_CARD_ID
        every { type } returns FlourishType.VINE
        every { primaryEffect } returns CardEffect.REPLAY_VINE
        every { matchEffect } returns CardEffect.DRAW_CARD
    }

    private val SUT = EffectReplayVine(mockChronicle)

    @BeforeEach
    fun setup() {
        fakePlayer.useDeckManager = false
        fakePlayer.cardsToPlay.clear()
    }

    @Test
    fun invoke_whenNoCardsInHand_doesNothing() {
        // Act
        SUT(fakePlayer)

        // Assert
        assertTrue(fakePlayer.cardsToPlay.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenNoVineCards_doesNothing() {
        // Arrange
        fakePlayer.addCardToHand(mockRootCard)

        // Act
        SUT(fakePlayer)

        // Assert
        assertTrue(fakePlayer.cardsToPlay.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_whenOnlyReplayVineCards_doesNothing() {
        // Arrange
        fakePlayer.addCardToHand(mockReplayVineCard)

        // Act
        SUT(fakePlayer)

        // Assert
        assertTrue(fakePlayer.cardsToPlay.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }

    @Test
    fun invoke_withSingleVineCard_addsToCardsToPlay() {
        // Arrange
        fakePlayer.addCardToHand(mockVineCard1)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(1, fakePlayer.cardsToPlay.size)
        assertEquals(mockVineCard1, fakePlayer.cardsToPlay[0])
        verify { mockChronicle(Moment.REPLAY_VINE(fakePlayer, mockVineCard1)) }
    }

    @Test
    fun invoke_withMultipleVineCards_addsFirstToCardsToPlay() {
        // Arrange
        fakePlayer.addCardToHand(mockVineCard1)
        fakePlayer.addCardToHand(mockVineCard2)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(1, fakePlayer.cardsToPlay.size)
        assertEquals(mockVineCard1, fakePlayer.cardsToPlay[0])
        verify { mockChronicle(Moment.REPLAY_VINE(fakePlayer, mockVineCard1)) }
    }

    @Test
    fun invoke_withMixedCardTypes_addsFirstVineCard() {
        // Arrange
        fakePlayer.addCardToHand(mockRootCard)
        fakePlayer.addCardToHand(mockVineCard1)
        fakePlayer.addCardToHand(mockReplayVineCard)

        // Act
        SUT(fakePlayer)

        // Assert
        assertEquals(1, fakePlayer.cardsToPlay.size)
        assertEquals(mockVineCard1, fakePlayer.cardsToPlay[0])
        verify { mockChronicle(Moment.REPLAY_VINE(fakePlayer, mockVineCard1)) }
    }

    @Test
    fun invoke_whenVineCardHasReplayVineAsMatchEffect_doesNotAddToCardsToPlay() {
        // Arrange
        val mockVineWithReplayMatch: GameCard = mockk {
            every { id } returns VINE_CARD_1_ID
            every { type } returns FlourishType.VINE
            every { primaryEffect } returns CardEffect.DRAW_CARD
            every { matchEffect } returns CardEffect.REPLAY_VINE
        }
        fakePlayer.addCardToHand(mockVineWithReplayMatch)

        // Act
        SUT(fakePlayer)

        // Assert
        assertTrue(fakePlayer.cardsToPlay.isEmpty())
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
