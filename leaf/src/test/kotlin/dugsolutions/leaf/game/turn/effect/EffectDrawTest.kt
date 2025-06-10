package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EffectDrawTest {

    companion object {
        private const val CARD_ID = 1
    }

    private val mockPlayer: Player = mockk(relaxed = true)
    private val mockCardManager: CardManager = mockk(relaxed = true)
    private val mockCard: GameCard = mockk(relaxed = true)

    private val SUT: EffectDraw = EffectDraw(mockCardManager)

    @BeforeEach
    fun setup() {
        every { mockPlayer.cardsToPlay } returns mutableListOf()
    }

    @Test
    fun invoke_whenMoreCardsThanDice_drawsCardAndAddsToCardsToPlay() {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 2
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.drawCard() } returns CARD_ID
        every { mockCardManager.getCard(CARD_ID) } returns mockCard

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.drawCard() }
        verify { mockCardManager.getCard(CARD_ID) }
        assertEquals(1, mockPlayer.cardsToPlay.size)
        assertEquals(mockCard, mockPlayer.cardsToPlay[0])
    }

    @Test
    fun invoke_whenMoreCardsThanDiceButDrawFails_doesNotAddToCardsToPlay() {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 2
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.drawCard() } returns null

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.drawCard() }
        verify(exactly = 0) { mockCardManager.getCard(any<CardID>()) }
        assertTrue(mockPlayer.cardsToPlay.isEmpty())
    }

    @Test
    fun invoke_whenMoreCardsThanDiceButCardNotFound_doesNotAddToCardsToPlay() {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 2
        every { mockPlayer.diceInSupplyCount } returns 1
        every { mockPlayer.drawCard() } returns CARD_ID
        every { mockCardManager.getCard(CARD_ID) } returns null

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.drawCard() }
        verify { mockCardManager.getCard(CARD_ID) }
        assertTrue(mockPlayer.cardsToPlay.isEmpty())
    }

    @Test
    fun invoke_whenMoreDiceThanCards_drawsDie() {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.diceInSupplyCount } returns 2

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockCardManager.getCard(any<CardID>()) }
        assertTrue(mockPlayer.cardsToPlay.isEmpty())
    }

    @Test
    fun invoke_whenEqualCardsAndDice_drawsDie() {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 1
        every { mockPlayer.diceInSupplyCount } returns 1

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockCardManager.getCard(any<CardID>()) }
        assertTrue(mockPlayer.cardsToPlay.isEmpty())
    }

    @Test
    fun invoke_whenNoCardsOrDice_drawsDie() {
        // Arrange
        every { mockPlayer.cardsInSupplyCount } returns 0
        every { mockPlayer.diceInSupplyCount } returns 0

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.drawDie() }
        verify(exactly = 0) { mockPlayer.drawCard() }
        verify(exactly = 0) { mockCardManager.getCard(any<CardID>()) }
        assertTrue(mockPlayer.cardsToPlay.isEmpty())
    }
} 
