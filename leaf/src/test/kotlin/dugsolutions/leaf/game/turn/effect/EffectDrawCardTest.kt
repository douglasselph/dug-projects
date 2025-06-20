package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
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
    fun invoke_fromCompostTrue_drawsFromCompost_andCallsChronicle() {
        // Arrange
        every { mockPlayer.drawCardFromBed() } returns CARD_ID
        every { mockCardManager.getCard(CARD_ID) } returns fakeCard

        // Act
        SUT(mockPlayer, fromCompost = true)

        // Assert
        verify { mockChronicle(Moment.DRAW_CARD(mockPlayer, CARD_ID)) }
        assertEquals(fakeCard, cardsToPlay[0])
    }

    @Test
    fun invoke_fromCompostFalse_drawsFromDeck_andCallsChronicle() {
        // Arrange
        every { mockPlayer.drawCard() } returns CARD_ID
        every { mockCardManager.getCard(CARD_ID) } returns fakeCard

        // Act
        SUT(mockPlayer, fromCompost = false)

        // Assert
        verify { mockChronicle(Moment.DRAW_CARD(mockPlayer, CARD_ID)) }
        assertEquals(fakeCard, cardsToPlay[0])
    }

    @Test
    fun invoke_whenNoCardDrawn_doesNothing() {
        // Arrange
        every { mockPlayer.drawCard() } returns null
        every { mockPlayer.drawCardFromBed() } returns null

        // Act
        SUT(mockPlayer, fromCompost = false)
        SUT(mockPlayer, fromCompost = true)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
