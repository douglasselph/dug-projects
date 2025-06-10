package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectCardToRetainTest {

    private val mockPlayer = mockk<Player>(relaxed = true)
    private val mockSelectCardToRetain: SelectCardToRetain = mockk(relaxed = true)
    private val card = mockk<GameCard>(relaxed = true)
    private val chronicle: GameChronicle = mockk(relaxed = true)

    private val SUT: EffectCardToRetain = EffectCardToRetain(mockSelectCardToRetain, chronicle)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun invoke_whenCardSelected_retainsCard_andCallsChronicle() {
        // Arrange
        every { mockSelectCardToRetain(mockPlayer.cardsInHand, null) } returns card

        // Act
        SUT(mockPlayer)

        // Assert
        verify { mockPlayer.retainCard(card) }
        verify { chronicle(Moment.RETAIN_CARD(mockPlayer, card)) }
    }

    @Test
    fun invoke_whenNoCardSelected_doesNothing() {
        // Arrange
        every { mockSelectCardToRetain(mockPlayer.cardsInHand, null) } returns null

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { chronicle(any()) }
    }
} 
