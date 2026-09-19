package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.FakeCards
import dugsolutions.leaf.v14.cards.domain.GameCard
import dugsolutions.leaf.v14.chronicle.GameChronicle
import dugsolutions.leaf.v14.chronicle.domain.Moment
import dugsolutions.leaf.v14.game.turn.effect.EffectDrawCard
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.domain.DrawCardResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectDrawCardTest {

    companion object {
        private val fakeCard = FakeCards.flowerCard
        private val CARD_ID = fakeCard.id
    }

    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockCardManager: CardManager = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)
    private val cardsToPlay = mutableListOf<GameCard>()

    private val SUT: EffectDrawCard = EffectDrawCard(mockCardManager, mockChronicle)

    @BeforeEach
    fun setup() {
        every { mockPlayer.cardsToPlay } returns cardsToPlay
    }

    @Test
    fun invoke_fromDiscardTrue_drawsFromDiscard_andCallsChronicle() {
        // Arrange
        every { mockPlayer.drawCardFromDiscard() } returns DrawCardResult(CARD_ID)
        every { mockCardManager.getCard(CARD_ID) } returns fakeCard

        // Act
        SUT(mockPlayer, fromDiscard = true)

        // Assert
        verify { mockChronicle(Moment.DRAW_CARD(mockPlayer, CARD_ID)) }
        assertEquals(fakeCard, cardsToPlay[0])
    }

    @Test
    fun invoke_fromDiscardFalse_drawsFromDeck_andCallsChronicle() {
        // Arrange
        every { mockPlayer.drawCard() } returns DrawCardResult(CARD_ID)
        every { mockCardManager.getCard(CARD_ID) } returns fakeCard

        // Act
        SUT(mockPlayer, fromDiscard = false)

        // Assert
        verify { mockChronicle(Moment.DRAW_CARD(mockPlayer, CARD_ID)) }
        assertEquals(fakeCard, cardsToPlay[0])
    }

    @Test
    fun invoke_whenNoCardDrawn_doesNothing() {
        // Arrange
        every { mockPlayer.drawCard() } returns DrawCardResult()
        every { mockPlayer.drawCardFromDiscard() } returns DrawCardResult()

        // Act
        SUT(mockPlayer, fromDiscard = false)
        SUT(mockPlayer, fromDiscard = true)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
