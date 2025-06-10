package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.HandItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectReuseCardTest {

    private val mockSelectCardToRetain: SelectCardToRetain = mockk(relaxed = true)
    private val mockChronicle: GameChronicle = mockk(relaxed = true)

    private val SUT: EffectReuseCard = EffectReuseCard(mockSelectCardToRetain, mockChronicle)

    private val mockCard: GameCard = mockk(relaxed = true)
    private val mockPlayer = mockk<Player>(relaxed = true)
    private val reused = mutableListOf<HandItem>()

    @BeforeEach
    fun setup() {
        every { mockPlayer.reused } returns reused
    }

    @Test
    fun invoke_whenCardSelected_addsToReused_andCallsChronicle() {
        // Arrange
        every { mockSelectCardToRetain(mockPlayer.cardsInHand) } returns mockCard

        // Act
        SUT(mockPlayer)

        // Assert
        assertEquals(HandItem.aCard(mockCard), reused[0])
        verify { mockChronicle(Moment.REUSE_CARD(mockPlayer, mockCard)) }
    }

    @Test
    fun invoke_whenNoCardSelected_doesNothing() {
        // Arrange
        every { mockSelectCardToRetain(mockPlayer.cardsInHand) } returns null

        // Act
        SUT(mockPlayer)

        // Assert
        verify(exactly = 0) { mockChronicle(any()) }
    }
} 
